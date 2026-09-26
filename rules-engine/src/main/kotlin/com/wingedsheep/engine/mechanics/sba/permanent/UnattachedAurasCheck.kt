package com.wingedsheep.engine.mechanics.sba.permanent

import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.unattachEmittingEvent
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.handlers.effects.permanent.attachments.AttachmentMover
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.SbaZoneMovementHelper
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentHostLeftComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * 704.5m - An Aura attached to an illegal object/player or not attached goes to graveyard.
 * 704.5n - An Equipment or Fortification attached to an illegal permanent becomes unattached
 *          but remains on the battlefield. This drives two Equipment cases below, both asked
 *          of the projected state (so layer-4 type-changing effects are seen):
 *            - The host stops being a creature (an Equipment can only equip a creature, CR
 *              301.5). E.g. the equipped creature is turned into a land, or an animated
 *              artifact's "until end of turn" animation wears off while still equipped.
 *            - The Equipment itself becomes a creature, so it can't legally equip another
 *              creature unless it has reconfigure (CR 301.5c). E.g. Atomic Microsizer turned
 *              into a 0/0 Robot artifact creature by Tezzeret, Cruel Captain's emblem.
 *            - The host stops matching the Equipment's own "can be attached only to …"
 *              restriction (Konda's Banner on a creature that stops being legendary).
 * 704.5p - A battle or creature attached to an object or player becomes unattached but
 *          remains on the battlefield.
 *
 * Enchant restrictions (CR 303.4c): an Aura's "Enchant …" ability restricts what it can *stay*
 * attached to, not just what its spell could target — the restriction is checked continuously, so
 * an Aura whose host stops matching it becomes an illegal attachment and is put into its owner's
 * graveyard by 704.5m. The Cartouche of Solidarity ruling puts it plainly: "If another player gains
 * control of either the Cartouche or the enchanted creature (but not both), then the Cartouche will
 * be enchanting an illegal permanent and be put into its owner's graveyard as a state-based action."
 * The "but not both" is why this compares the Aura's controller against the host's rather than
 * watching for a control *change* — if one player ends up with both, the attachment is legal again.
 * A host that stops being a creature at all (Pacifism on a permanent Imprisoned in the Moon turned
 * into a land) is the same story via the type predicate. The restriction is re-read from the card's
 * `auraTarget` and evaluated against the *projected* host so layer-4 type/control changes are seen.
 *
 * Protection (CR 702.16c/d): a permanent with protection from a quality can't be enchanted by
 * Auras (put into their owners' graveyards as a state-based action) or equipped by Equipment
 * (becomes unattached, stays on the battlefield) that have the stated quality. This covers
 * protection gained *after* the attachment landed — targeting-time protection is enforced by
 * `TargetValidator`. An attachment whose own printed ability grants that very protection to
 * its host is exempt ("This effect doesn't remove this Aura", the Ward cycle).
 */
class UnattachedAurasCheck(
    private val zones: ZoneTransitionService,
    private val cardRegistry: CardRegistry
) : StateBasedActionCheck {
    private val predicateEvaluator = zones.predicateEvaluator
    override val name = "704.5m/n/p Unattached Auras"
    override val order = SbaOrder.UNATTACHED_AURAS

    override fun check(state: GameState): ExecutionResult {
        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()
        val projected = state.projectedState

        for (entityId in state.getBattlefield().toList()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // CR 310.10 / 704.5p: a battle can't be attached to anything, even if it is also an Aura
            // or Equipment. It becomes unattached and stays on the battlefield, and the Aura rules
            // below never apply to it (an unattached Aura-battle is not put into the graveyard).
            if (projected.isBattle(entityId)) {
                if (container.has<AttachedToComponent>()) {
                    val (detached, unattachEvents) = unattachEmittingEvent(newState, entityId)
                    newState = detached
                    events.addAll(unattachEvents)
                }
                continue
            }

            val isAura = cardComponent.typeLine.isAura
            val isEquipment = cardComponent.typeLine.isEquipment

            if (!isAura && !isEquipment) continue

            // CR 400.7 / 704.5m-n: the host this attachment was on left the battlefield. The host's
            // EntityId may have returned via a blink (a same-id but *new* object), so the id-based
            // checks below can't see the leave — the leave-time marker does. An Aura goes to the
            // graveyard; an Equipment unattaches and stays on the battlefield.
            val hostLeft = container.get<AttachmentHostLeftComponent>()
            if (hostLeft != null) {
                newState = newState.updateEntity(entityId) { c -> c.without<AttachmentHostLeftComponent>() }
                // Only act on the marker while the attachment is *still* pointing at the host that
                // left (or is already unattached) — the marker exists for the blink case where the
                // host returns under the same EntityId. If an effect has re-attached it to a
                // different permanent in the meantime, both 704.5m and 704.5n are silent: the
                // attachment is neither unattached nor (necessarily) illegally attached, so the
                // ordinary legality checks below judge the *new* host instead. Two printed cards
                // do exactly this inside one resolution — Zack Fair attaches "an Equipment that was
                // attached to it" to another creature as it is sacrificed, and Breath of Fury
                // sacrifices its own enchanted creature and attaches itself to another creature
                // you control. Applying the marker blindly would put a legally-attached Aura into
                // its owner's graveyard.
                val current = container.get<AttachedToComponent>()
                if (current == null || current.targetId == hostLeft.lastKnownHostId) {
                    if (isAura) {
                        // CR 704.5m: an Aura whose host left is put into its owner's graveyard.
                        val result = SbaZoneMovementHelper.putPermanentInGraveyard(
                            zones,
                            newState, entityId, cardComponent,
                            lastKnownAttachedTo = hostLeft.lastKnownHostId
                        )
                        newState = result.newState
                        events.addAll(result.events)
                    } else {
                        // CR 704.5n: an Equipment whose host left becomes unattached but stays on
                        // the battlefield.
                        val (detached, unattachEvents) = unattachEmittingEvent(newState, entityId)
                        newState = detached
                        events.addAll(unattachEvents)
                    }
                    continue
                }
            }

            val attachedTo = container.get<AttachedToComponent>()
            if (attachedTo == null) {
                if (isAura) {
                    // Aura not attached to anything - goes to graveyard
                    val result = SbaZoneMovementHelper.putPermanentInGraveyard(
                        zones,
                        newState, entityId, cardComponent
                    )
                    newState = result.newState
                    events.addAll(result.events)
                }
                // Equipment not attached to anything is fine - stays on battlefield
            } else if (isAura && attachedTo.targetId in state.turnOrder) {
                // 704.5m — an "enchant player" Aura (Grievous Wound) is attached to a player, not
                // a battlefield permanent. It stays as long as that player is still in the game;
                // once the player leaves, PlayerLeavesGameProcessor removes them from turnOrder and
                // the next check sends the now-unattached Aura to the graveyard.
                continue
            } else {
                // Check if attached target still exists on battlefield
                if (attachedTo.targetId !in state.getBattlefield()) {
                    if (isAura) {
                        // Aura's target gone - goes to graveyard
                        val result = SbaZoneMovementHelper.putPermanentInGraveyard(
                            zones,
                            newState, entityId, cardComponent,
                            lastKnownAttachedTo = attachedTo.targetId
                        )
                        newState = result.newState
                        events.addAll(result.events)
                    } else {
                        // Equipment's target gone - just detach, stays on battlefield
                        val (detached, unattachEvents) = unattachEmittingEvent(newState, entityId)
                        newState = detached
                        events.addAll(unattachEvents)
                    }
                } else if (
                    isEquipment && (
                        // CR 704.5n: the host is no longer a legal permanent for an Equipment.
                        // An Equipment can only be attached to a creature, so once the host
                        // stops being a creature (turned into a land, animation wore off, etc.)
                        // the attachment is illegal and the Equipment unattaches.
                        !projected.isCreature(attachedTo.targetId) ||
                        // CR 301.5c / 704.5n: the Equipment itself became a creature, so it
                        // can't equip a creature unless it has reconfigure.
                        (projected.isCreature(entityId) &&
                            !projected.hasKeyword(entityId, "RECONFIGURE")) ||
                        // The host no longer matches the Equipment's own attach restriction.
                        !AttachmentMover.equipRestrictionAllows(
                            state, predicateEvaluator, cardRegistry, entityId, attachedTo.targetId
                        )
                    )
                ) {
                    // Illegal attachment: the Equipment unattaches but stays on the battlefield.
                    val (detached, unattachEvents) = unattachEmittingEvent(newState, entityId)
                    newState = detached
                    events.addAll(unattachEvents)
                } else if (
                    isAura && hostFailsEnchantRestriction(state, projected, entityId, cardComponent, attachedTo.targetId)
                ) {
                    // CR 303.4c / 704.5m: the host no longer matches this Aura's "Enchant …"
                    // restriction (control changed hands, the host stopped being a creature, …),
                    // so the Aura is illegally attached and goes to its owner's graveyard.
                    val result = SbaZoneMovementHelper.putPermanentInGraveyard(
                        zones,
                        newState, entityId, cardComponent,
                        lastKnownAttachedTo = attachedTo.targetId
                    )
                    newState = result.newState
                    events.addAll(result.events)
                } else if (
                    hostProtectedFromAttachmentColor(newState, projected, entityId, cardComponent, attachedTo.targetId)
                ) {
                    // CR 702.16c/d: the host has protection from one of this attachment's colors
                    // (gained after the attachment landed — e.g. White Ward's pro-white sends an
                    // already-attached Holy Strength to the graveyard). Aura -> owner's graveyard
                    // (704.5m); Equipment -> unattaches, stays on the battlefield (704.5n).
                    if (isAura) {
                        val result = SbaZoneMovementHelper.putPermanentInGraveyard(
                            zones,
                            newState, entityId, cardComponent,
                            lastKnownAttachedTo = attachedTo.targetId
                        )
                        newState = result.newState
                        events.addAll(result.events)
                    } else {
                        val (detached, unattachEvents) = unattachEmittingEvent(newState, entityId)
                        newState = detached
                        events.addAll(unattachEvents)
                    }
                }
            }
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * True when [hostId] no longer satisfies the Aura's printed "Enchant …" restriction (CR 303.4c).
     *
     * Only the requirement's *filter* is re-evaluated — not full targeting legality. An attached
     * Aura isn't re-targeted, so hexproof/shroud/"can't be the target of" gained after the fact
     * don't dislodge it (CR 702.11b); protection is the one quality that does, and
     * [hostProtectedFromAttachmentColor] handles it separately.
     *
     * Deliberately fails *open* — an Aura we can't judge (printing not in the registry, an
     * "enchant player" requirement, a filter scoped to a zone other than the battlefield) is left
     * attached rather than destroyed, because a wrong verdict here silently removes a card from
     * the game.
     */
    private fun hostFailsEnchantRestriction(
        state: GameState,
        projected: ProjectedState,
        auraId: EntityId,
        auraCard: CardComponent,
        hostId: EntityId
    ): Boolean {
        val requirement = cardRegistry.getCard(auraCard.cardDefinitionId)?.script?.auraTarget ?: return false
        // "you" in "Enchant creature you control" is the Aura's controller, read from the
        // projection so a control-changing effect on the Aura itself is honored.
        val controllerId = projected.getController(auraId) ?: return false
        val satisfied = com.wingedsheep.engine.handlers.predicates.EnchantRestriction.hostSatisfies(
            state, projected, predicateEvaluator, requirement, hostId, controllerId, auraId
        ) ?: return false
        return !satisfied
    }

    /** CR 702.16c/d — see [com.wingedsheep.engine.handlers.predicates.EnchantRestriction.hostProtectedFromAttachmentColor]. */
    private fun hostProtectedFromAttachmentColor(
        state: GameState,
        projected: ProjectedState,
        attachmentId: EntityId,
        attachmentCard: CardComponent,
        hostId: EntityId
    ): Boolean = com.wingedsheep.engine.handlers.predicates.EnchantRestriction.hostProtectedFromAttachmentColor(
        state, projected, cardRegistry, attachmentId, attachmentCard, hostId
    )
}

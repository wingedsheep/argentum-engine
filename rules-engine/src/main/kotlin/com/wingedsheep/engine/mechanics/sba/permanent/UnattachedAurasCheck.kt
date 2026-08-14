package com.wingedsheep.engine.mechanics.sba.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.unattachEmittingEvent
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.SbaZoneMovementHelper
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentHostLeftComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GrantProtection
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther
import com.wingedsheep.sdk.scripting.targets.TargetRequirement

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
    private val cardRegistry: CardRegistry
) : StateBasedActionCheck {
    override val name = "704.5m/n/p Unattached Auras"
    override val order = SbaOrder.UNATTACHED_AURAS

    private val predicateEvaluator = PredicateEvaluator()

    override fun check(state: GameState): ExecutionResult {
        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()
        val projected = state.projectedState

        for (entityId in state.getBattlefield().toList()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

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
                if (isAura) {
                    val result = SbaZoneMovementHelper.putPermanentInGraveyard(
                        newState, entityId, cardComponent,
                        lastKnownAttachedTo = hostLeft.lastKnownHostId
                    )
                    newState = result.newState
                    events.addAll(result.events)
                } else {
                    // CR 704.5n: an Equipment whose host left becomes unattached but stays on the
                    // battlefield. Only force the unattach when it's still pointing at the host that
                    // left (or is already unattached) — the marker exists for the blink case where the
                    // host returns under the same EntityId. If an effect has *re-attached* it to a
                    // different permanent in the meantime (e.g. Zack Fair attaches "an Equipment that
                    // was attached to it" to another creature as it's sacrificed), leave that new
                    // attachment in place; the legality checks below validate the new host instead.
                    val current = container.get<AttachedToComponent>()
                    if (current == null || current.targetId == hostLeft.lastKnownHostId) {
                        val (detached, unattachEvents) = unattachEmittingEvent(newState, entityId)
                        newState = detached
                        events.addAll(unattachEvents)
                    }
                }
                continue
            }

            val attachedTo = container.get<AttachedToComponent>()
            if (attachedTo == null) {
                if (isAura) {
                    // Aura not attached to anything - goes to graveyard
                    val result = SbaZoneMovementHelper.putPermanentInGraveyard(
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
                            !projected.hasKeyword(entityId, "RECONFIGURE"))
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
                        newState, entityId, cardComponent,
                        lastKnownAttachedTo = attachedTo.targetId
                    )
                    newState = result.newState
                    events.addAll(result.events)
                } else if (
                    hostProtectedFromAttachmentColor(projected, entityId, cardComponent, attachedTo.targetId)
                ) {
                    // CR 702.16c/d: the host has protection from one of this attachment's colors
                    // (gained after the attachment landed — e.g. White Ward's pro-white sends an
                    // already-attached Holy Strength to the graveyard). Aura -> owner's graveyard
                    // (704.5m); Equipment -> unattaches, stays on the battlefield (704.5n).
                    if (isAura) {
                        val result = SbaZoneMovementHelper.putPermanentInGraveyard(
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
        val filter = enchantFilter(requirement) ?: return false
        // "you" in "Enchant creature you control" is the Aura's controller, read from the
        // projection so a control-changing effect on the Aura itself is honored.
        val controllerId = projected.getController(auraId) ?: return false
        val context = PredicateContext(controllerId = controllerId, sourceId = auraId)
        // A cross-zone union requirement is satisfied by any one clause; only battlefield clauses
        // can describe a host an Aura is attached to.
        val battlefieldClauses = filter.clauses().filter { it.zone == Zone.BATTLEFIELD }
        if (battlefieldClauses.isEmpty()) return false
        return battlefieldClauses.none {
            predicateEvaluator.matches(state, projected, hostId, it.baseFilter, context)
        }
    }

    /**
     * The battlefield filter behind an Aura's `auraTarget`, or null when the requirement isn't one
     * we can re-check against a permanent host (an "enchant player" [TargetRequirement], say).
     */
    private fun enchantFilter(
        requirement: TargetRequirement
    ): com.wingedsheep.sdk.scripting.filters.unified.TargetFilter? = when (requirement) {
        is TargetObject -> requirement.filter
        // "Enchant another …" — the distinctness rule is targeting-only; the filter is the base's.
        is TargetOther -> enchantFilter(requirement.baseRequirement)
        else -> null
    }

    /**
     * True when the attached permanent's host has protection from one of the attachment's
     * (projected) colors, CR 702.16c/d. An attachment whose own printed [GrantProtection]
     * grants that color's protection is exempt — the Ward cycle's "This effect doesn't remove
     * this Aura". (Approximation: the exemption is per-color rather than per-effect, so two
     * same-color Wards on one host both survive where strict rules would remove each via the
     * other's effect — an untracked-provenance corner case.)
     */
    private fun hostProtectedFromAttachmentColor(
        projected: ProjectedState,
        attachmentId: EntityId,
        attachmentCard: CardComponent,
        hostId: EntityId
    ): Boolean {
        val colors = projected.getColors(attachmentId)
        if (colors.isEmpty()) return false
        val statics = cardRegistry.getCard(attachmentCard.cardDefinitionId)
            ?.staticAbilities
            .orEmpty()
        // Dynamic protection grants (chosen color, colors of controlled permanents) can cover
        // any color at any time — exempt the attachment from protection-removal entirely
        // (Pledge of Loyalty's "This effect doesn't remove Pledge of Loyalty").
        if (statics.any {
                it is com.wingedsheep.sdk.scripting.GrantProtectionFromControlledColors ||
                    it is com.wingedsheep.sdk.scripting.GrantProtectionFromChosenColorToGroup
            }
        ) return false
        val selfGrantedColors: Set<Color> = statics
            .filterIsInstance<GrantProtection>()
            .map { it.color }
            .toSet()
        return Color.entries.any { color ->
            color.name in colors &&
                color !in selfGrantedColors &&
                projected.hasKeyword(hostId, "PROTECTION_FROM_${color.name}")
        }
    }
}

package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.consumeShieldCounter
import com.wingedsheep.engine.core.tap
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedEverComponent
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentHostLeftComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.DamageDealtToCreaturesThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.HasDealtCombatDamageToPlayerComponent
import com.wingedsheep.engine.state.components.battlefield.HasBecomeTappedComponent
import com.wingedsheep.engine.state.components.battlefield.HasDealtDamageComponent
import com.wingedsheep.engine.state.components.battlefield.WasDealtDamageThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.ExileOnLeaveBattlefieldComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.battlefield.CastFromHandComponent
import com.wingedsheep.engine.state.components.battlefield.WarpedComponent
import com.wingedsheep.engine.state.components.battlefield.EvokedComponent
import com.wingedsheep.engine.state.components.battlefield.CastRecordComponent
import com.wingedsheep.engine.state.components.battlefield.CraftedFromExiledComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.battlefield.MayCastFromLinkedExileUsedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.TimestampComponent
import com.wingedsheep.engine.state.components.combat.AttackerOrderComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.combat.DamageAssignmentComponent
import com.wingedsheep.engine.state.components.combat.DamageAssignmentOrderComponent
import com.wingedsheep.engine.state.components.combat.DealtFirstStrikeDamageComponent
import com.wingedsheep.engine.state.components.combat.RequiresManualDamageAssignmentComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.engine.mechanics.MadnessGrants
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.RedirectZoneChangeWithEffect
import com.wingedsheep.sdk.scripting.ZoneChangeCause
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.predicates.evaluateWith

/**
 * Result of a zone change redirect check.
 *
 * @param destinationZone The (possibly redirected) destination zone
 * @param additionalEffect An optional effect to execute when the redirect applies
 *        (e.g., TakeExtraTurnEffect from Ugin's Nexus)
 * @param effectControllerId The controller of the replacement effect source
 * @param effectSourceId The permanent that hosts the replacement effect. Passed to
 *        [ZoneMovementUtils.applyReplacementAdditionalEffect] as the rider's source so a created
 *        token resolves the minting set's art (Head of the Hunt's Wolf), exactly as it would if
 *        the rider had resolved as an ability. May already be off the battlefield by then — the
 *        replacement applied while it was still there (CR 704.3).
 * @param linkSourceId When non-null (and [destinationZone] is [Zone.EXILE]), the redirected
 *        card should be linked to this source permanent's `LinkedExileComponent` after the move
 *        — set by a [RedirectZoneChange] with `linkToSource = true` (Valgavoth, Terror Eater).
 *        Each mover applies the link via [ZoneMovementUtils.linkExiledToSource].
 * @param shuffleIntoLibrary When true (and [destinationZone] is [Zone.LIBRARY]), the card is
 *        shuffled into its owner's library rather than placed on top — Darksteel Colossus /
 *        Progenitus. Movers translate this into [LibraryPlacement.Shuffled].
 * @param reveal When true, the card is revealed as it is redirected (informational — the shuffle
 *        destination is otherwise hidden). Darksteel Colossus / Progenitus.
 */
data class ZoneChangeRedirectResult(
    val destinationZone: Zone,
    val additionalEffect: com.wingedsheep.sdk.scripting.effects.Effect? = null,
    val effectControllerId: EntityId? = null,
    val effectSourceId: EntityId? = null,
    val linkSourceId: EntityId? = null,
    val shuffleIntoLibrary: Boolean = false,
    val reveal: Boolean = false
)

/**
 * Utility functions for zone transitions: moving permanents between zones,
 * destroying permanents, and cleaning up battlefield state.
 *
 * Handles combat reference cleanup, attachment cleanup, floating effect removal,
 * component stripping, zone change redirection, and regeneration.
 */
object ZoneMovementUtils {

    private val predicateEvaluator = PredicateEvaluator()

    /**
     * Token executor used by [applyReplacementAdditionalEffect] for a replacement's
     * "…instead. When you do, create a token" rider (Head of the Hunt).
     *
     * Wired by [com.wingedsheep.engine.core.EngineServices] so the rider mints through the same
     * executor an ability would, with the card and token-art registries attached — mirroring
     * `ZoneTransitionService.cardRegistry`. The default stand-in keeps this object usable
     * unwired (unit tests, gym rollouts); tokens then fall back to the engine-wide generic art
     * for their creature type.
     */
    var tokenExecutor: com.wingedsheep.engine.handlers.effects.token.CreateTokenExecutor =
        com.wingedsheep.engine.handlers.effects.token.CreateTokenExecutor()

    /**
     * Destinations that the commander zone-change replacement can intercept (CR 903.9).
     * Battlefield, stack, and command itself are intentionally excluded — commanders enter
     * the battlefield like any other permanent, can sit on the stack while resolving, and
     * "moving to the command zone" while already there is a no-op.
     */
    private val COMMANDER_DIVERT_DESTINATIONS = setOf(
        Zone.GRAVEYARD,
        Zone.EXILE,
        Zone.HAND,
        Zone.LIBRARY,
    )

    /**
     * Apply Saga entry setup to an entity entering the battlefield (Rule 714.3a).
     * Adds SagaComponent with chapter 1 marked as triggered, and adds an initial lore counter.
     *
     * @return Pair of (updated state, list of events to emit) — empty events if not a Saga
     */
    fun applySagaEntryIfNeeded(
        state: GameState,
        entityId: EntityId
    ): Pair<GameState, List<EngineGameEvent>> {
        val container = state.getEntity(entityId) ?: return state to emptyList()
        val cardComponent = container.get<CardComponent>() ?: return state to emptyList()
        if (!cardComponent.typeLine.isSaga) return state to emptyList()

        val current = container.get<CountersComponent>() ?: CountersComponent()
        val sagaComponent = SagaComponent(triggeredChapters = setOf(1))
        val newState = state.updateEntity(entityId) { c ->
            c.with(sagaComponent)
                .with(current.withAdded(CounterType.LORE, 1))
        }
        return newState to listOf(CountersAddedEvent(entityId, "LORE", 1, cardComponent.name))
    }

    /**
     * Apply the *intrinsic* enters-with-counters ability of an entity entering the battlefield —
     * a planeswalker's loyalty (CR 306.5b) or a battle's defense (CR 310.4b). Both are worded the
     * same way ("this permanent enters with a number of X counters on it equal to its printed X
     * number") and both are replacement effects (CR 614.1c), so they apply to *every* way the
     * permanent enters, not just a resolving spell. Without this, a planeswalker or battle put onto
     * the battlefield from any non-stack zone (returned by an O-Ring style "until this leaves"
     * exile, reanimated, tutored straight into play) would enter with 0 counters and be put into
     * its owner's graveyard by state-based actions (CR 704.5i / 704.5v/w) the moment it arrived.
     *
     * Called from both battlefield-entry pipelines, exactly like [applySagaEntryIfNeeded]:
     * [ZoneTransitionService.moveToZone] for every zone-change entry, and the ad-hoc
     * [BattlefieldEntry.place] token-minting paths for a token copy (printed loyalty and printed
     * defense are copiable values per CR 707.2, so the token has one to place). The cast pipeline
     * reaches the same counters through [EntersWithReplacements.placeEntryCounters] while the
     * permanent is still on the stack (see
     * [com.wingedsheep.engine.mechanics.stack.StackResolver]), so no entry gets them twice.
     *
     * The number is read from the *current* [CardComponent]'s definition, so a double-faced card
     * returning transformed onto its planeswalker back face gets the back face's printed loyalty
     * (its `cardDefinitionId` has already been flipped by then).
     *
     * Face-down entries are skipped: a face-down permanent is a nameless 2/2 creature with no
     * printed loyalty or defense (CR 708.2a). The check lives here rather than at the call sites so
     * every caller inherits it.
     *
     * Routed through [EntersWithReplacements.placeEntryCounters] so counter-placement modifiers
     * (Doubling Season, Vorinclex, Pir) and the "a counter was placed this turn" tracker behave
     * exactly as they do for a printed "enters with counters".
     *
     * @return Pair of (updated state, events to emit) — empty events if the permanent has no
     *   intrinsic entry counters
     */
    fun applyIntrinsicEntryCountersIfNeeded(
        state: GameState,
        entityId: EntityId,
        controllerId: EntityId,
        cardRegistry: CardRegistry
    ): Pair<GameState, List<EngineGameEvent>> {
        val container = state.getEntity(entityId) ?: return state to emptyList()
        if (container.has<FaceDownComponent>()) return state to emptyList()
        val cardComponent = container.get<CardComponent>() ?: return state to emptyList()
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
        val (filter, amount) = when {
            cardComponent.isPlaneswalker -> CounterTypeFilter.Loyalty to cardDef?.startingLoyalty
            cardComponent.isBattle ->
                com.wingedsheep.engine.mechanics.battle.Battles.DEFENSE_COUNTER to cardDef?.startingDefense
            else -> return state to emptyList()
        }
        if (amount == null) return state to emptyList()

        return EntersWithReplacements.placeEntryCounters(
            state, entityId, filter, amount, controllerId, cardComponent.name
        )
    }

    /**
     * Remove an entity from every battlefield permanent's [LinkedExileComponent].
     * Called when a card leaves the exile zone (cast, returned, blinked, etc.) so the
     * granter (e.g. Maralen, Fae Ascendant) no longer treats the now-departed card as
     * eligible for its linked-exile permission. Granters whose own LTB triggers consume
     * the link directly are unaffected — they leave the battlefield first, taking the
     * component with them.
     */
    fun unlinkFromAllLinkedExiles(state: GameState, leavingEntityId: EntityId): GameState {
        var newState = state
        for (entityId in state.getBattlefield()) {
            val container = newState.getEntity(entityId) ?: continue
            val linked = container.get<LinkedExileComponent>() ?: continue
            if (leavingEntityId !in linked.exiledIds) continue
            val remaining = linked.exiledIds.filter { it != leavingEntityId }
            newState = newState.updateEntity(entityId) { c ->
                c.with(LinkedExileComponent(remaining))
            }
        }
        return newState
    }

    /**
     * Append [exiledId] to [sourceId]'s [LinkedExileComponent]. Called by zone movers after a
     * [RedirectZoneChange] with `linkToSource = true` sends a card to exile (Valgavoth, Terror
     * Eater), so the source can later reference — and grant casting/playing of — the cards it
     * exiled. No-op if the source has left the battlefield, or the card is already linked.
     */
    fun linkExiledToSource(state: GameState, exiledId: EntityId, sourceId: EntityId): GameState {
        val container = state.getEntity(sourceId) ?: return state
        val existing = container.get<LinkedExileComponent>()?.exiledIds ?: emptyList()
        if (exiledId in existing) return state
        return state.updateEntity(sourceId) { c ->
            c.with(LinkedExileComponent(existing + exiledId))
        }
    }

    /**
     * Clean up combat references to a leaving entity on other creatures.
     * When a blocker leaves the battlefield, remove it from each attacker's
     * BlockedComponent.blockerIds and DamageAssignmentOrderComponent.orderedBlockers.
     * When an attacker leaves the battlefield, remove it from each blocker's
     * BlockingComponent.blockedAttackerIds and AttackerOrderComponent.orderedAttackers.
     */
    fun cleanupCombatReferences(state: GameState, leavingEntityId: EntityId): GameState {
        val container = state.getEntity(leavingEntityId) ?: return state
        var newState = state

        // If leaving entity is a blocker, clean up attackers' references to it
        val blockingComponent = container.get<BlockingComponent>()
        if (blockingComponent != null) {
            for (attackerId in blockingComponent.blockedAttackerIds) {
                newState = newState.updateEntity(attackerId) { attackerContainer ->
                    var updated = attackerContainer
                    val blocked = updated.get<BlockedComponent>()
                    if (blocked != null) {
                        updated = updated.with(BlockedComponent(blocked.blockerIds.filter { it != leavingEntityId }))
                    }
                    val order = updated.get<DamageAssignmentOrderComponent>()
                    if (order != null) {
                        updated = updated.with(DamageAssignmentOrderComponent(order.orderedBlockers.filter { it != leavingEntityId }))
                    }
                    updated
                }
            }
        }

        // If leaving entity is an attacker, clean up blockers' references to it
        val attackingComponent = container.get<AttackingComponent>()
        if (attackingComponent != null) {
            for ((entityId, components) in newState.entities) {
                val blocking = components.get<BlockingComponent>() ?: continue
                if (leavingEntityId in blocking.blockedAttackerIds) {
                    newState = newState.updateEntity(entityId) { blockerContainer ->
                        var updated = blockerContainer
                        val updatedBlocking = updated.get<BlockingComponent>()
                        if (updatedBlocking != null) {
                            val updatedIds = updatedBlocking.blockedAttackerIds - leavingEntityId
                            updated = if (updatedIds.isEmpty()) {
                                updated.without<BlockingComponent>()
                            } else {
                                updated.with(BlockingComponent(updatedIds))
                            }
                        }
                        val attackerOrder = updated.get<AttackerOrderComponent>()
                        if (attackerOrder != null) {
                            updated = updated.with(AttackerOrderComponent(attackerOrder.orderedAttackers.filter { it != leavingEntityId }))
                        }
                        updated
                    }
                }
            }
        }

        return newState
    }

    /**
     * Remove floating effects targeting an entity that is leaving the battlefield.
     * Per MTG Rule 400.7, a card that changes zones becomes a new object with no
     * memory of its previous existence — all floating effects targeting it should be removed.
     *
     * For effects that target multiple entities, the leaving entity is removed from
     * affectedEntities but the effect is kept alive for the remaining targets.
     * Effects that exclusively target the leaving entity are removed entirely.
     */
    fun removeFloatingEffectsTargeting(state: GameState, entityId: EntityId): GameState {
        val updatedEffects = state.floatingEffects.mapNotNull { floatingEffect ->
            if (entityId !in floatingEffect.effect.affectedEntities) {
                floatingEffect
            } else {
                val remaining = floatingEffect.effect.affectedEntities - entityId
                if (remaining.isEmpty()) {
                    null // Remove entirely — this effect only targeted the leaving entity
                } else {
                    floatingEffect.copy(
                        effect = floatingEffect.effect.copy(affectedEntities = remaining)
                    )
                }
            }
        }
        return if (updatedEffects.size == state.floatingEffects.size &&
            updatedEffects.zip(state.floatingEffects).all { (a, b) -> a === b }) {
            state // No changes
        } else {
            state.copy(floatingEffects = updatedEffects)
        }
    }

    /**
     * Clean up the reverse attachment link (AttachmentsComponent) on the permanent
     * that this entity was attached to. Called when an aura or equipment leaves the battlefield.
     */
    fun cleanupReverseAttachmentLink(state: GameState, entityId: EntityId): GameState {
        val attachedTo = state.getEntity(entityId)?.get<AttachedToComponent>() ?: return state
        return state.updateEntity(attachedTo.targetId) { container ->
            val attachments = container.get<AttachmentsComponent>()
            if (attachments != null) {
                val updatedIds = attachments.attachedIds.filter { it != entityId }
                if (updatedIds.isEmpty()) {
                    container.without<AttachmentsComponent>()
                } else {
                    container.with(AttachmentsComponent(updatedIds))
                }
            } else {
                container
            }
        }
    }

    /**
     * Break an attachment: clear the attachment's [AttachedToComponent], drop it from the host's
     * [AttachmentsComponent], and report the [PermanentUnattachedEvent] that drives "becomes
     * unattached" triggers (CR 701.3d).
     *
     * The single chokepoint for *staying-on-the-battlefield* unattaches — the explicit unattach
     * effect, the CR 704.5m/n state-based unattach, and re-equipping onto a different host all go
     * through it, so none of them can silently skip the event. (The attachment leaving the
     * battlefield is the fourth path; it lives in [ZoneTransitionService], which must emit the
     * event *before* [stripBattlefieldComponents] erases the link.)
     *
     * A no-op returning no events when the attachment isn't attached to anything, so callers can
     * invoke it unconditionally — an already-detached attachment emitted its event when it actually
     * came off, and must not emit a second one.
     */
    fun unattachEmittingEvent(
        state: GameState,
        attachmentId: EntityId,
    ): Pair<GameState, List<EngineGameEvent>> {
        val container = state.getEntity(attachmentId) ?: return state to emptyList()
        val formerHostId = container.get<AttachedToComponent>()?.targetId
            ?: return state to emptyList()

        var newState = cleanupReverseAttachmentLink(state, attachmentId)
        newState = newState.updateEntity(attachmentId) { c -> c.without<AttachedToComponent>() }

        return newState to listOf(
            com.wingedsheep.engine.core.PermanentUnattachedEvent(
                attachmentId = attachmentId,
                attachmentName = container.get<CardComponent>()?.name ?: "",
                attachedToId = formerHostId,
                // Read before the detach so a control-changing effect on the attachment is honored;
                // falls back to the owner for an attachment already off the battlefield.
                controllerId = container.get<ControllerComponent>()?.playerId
                    ?: container.get<CardComponent>()?.ownerId
                    ?: formerHostId,
            )
        )
    }

    /**
     * Mark every Aura/Equipment attached to a permanent that is leaving the battlefield so the
     * unattached-permanents state-based action ([UnattachedAurasCheck]) detaches/graveyards it
     * afterwards (CR 400.7 new object; 704.5n unattaches Equipment, 704.5m graveyards Auras).
     *
     * The host's EntityId is reused across a blink (exile → battlefield round-trip via
     * [ZoneTransitionService.moveToZone]), so the SBA's "host no longer on the battlefield" check is
     * defeated when a same-id object returns. This records the leave directly on each attachment
     * instead of relying on the host's id being absent. The attachment's [AttachedToComponent] is
     * deliberately left intact so the live `aurasByTarget` index still surfaces ATTACHED-binding
     * "when equipped creature dies/leaves" triggers (e.g. Forebears Blade) during detection.
     */
    fun markAttachmentsHostLeft(state: GameState, leavingHostId: EntityId): GameState {
        val attachedIds = state.getEntity(leavingHostId)
            ?.get<AttachmentsComponent>()?.attachedIds ?: return state
        var newState = state
        for (attachmentId in attachedIds) {
            val attachment = newState.getEntity(attachmentId) ?: continue
            // Only mark things still pointing at this host; an attachment already re-pointed or
            // detached by an earlier step in the same batch must not be dragged off.
            if (attachment.get<AttachedToComponent>()?.targetId != leavingHostId) continue
            newState = newState.updateEntity(attachmentId) { c ->
                c.with(AttachmentHostLeftComponent(lastKnownHostId = leavingHostId))
            }
        }
        return newState
    }

    /**
     * Strip all battlefield-specific components from an entity leaving the battlefield.
     * Per MTG Rule 400.7, when an object changes zones it becomes a new object with no
     * memory of its previous existence. This removes all transient battlefield state:
     * tapped, damage, counters, summoning sickness, combat state, attachments, etc.
     */
    fun stripBattlefieldComponents(container: ComponentContainer): ComponentContainer {
        return container
            // Identity
            .without<ControllerComponent>()
            .without<FaceDownComponent>()
            .without<com.wingedsheep.engine.state.components.identity.FaceDownModeComponent>()
            .without<MorphDataComponent>()
            .without<RevealedToComponent>()
            // Copy effects on permanents end when the object leaves the battlefield
            // (CR 400.7 / 707.2). ZoneTransitionService restores the printed
            // CardComponent before this strip runs.
            .without<com.wingedsheep.engine.state.components.identity.CopyOfComponent>()
            // The Ring-bearer designation is tied to the permanent; a permanent that leaves the
            // battlefield stops being the Ring-bearer (CR 701.54e), and the object that returns is
            // a new object (CR 400.7) that must not inherit the designation (e.g. a blinked
            // Ring-bearer via Meneldor, Swift Savior).
            .without<com.wingedsheep.engine.state.components.identity.RingBearerComponent>()
            // Battlefield
            .without<TappedComponent>()
            .without<SummoningSicknessComponent>()
            .without<CastFromHandComponent>()
            .without<com.wingedsheep.engine.state.components.battlefield.CastFromGraveyardComponent>()
            .without<com.wingedsheep.engine.state.components.battlefield.CastFromLibraryComponent>()
            .without<com.wingedsheep.engine.state.components.battlefield.EnteredFromGraveyardComponent>()
            .without<com.wingedsheep.engine.state.components.battlefield.CastFromExileComponent>()
            .without<com.wingedsheep.engine.state.components.battlefield.EnteredFromExileComponent>()
            .without<WarpedComponent>()
            .without<com.wingedsheep.engine.state.components.battlefield.DashedComponent>()
            .without<EvokedComponent>()
            // Cast-time choices (DynamicAmount.CastX / CastChoice, chosen color/type/mode, kicked)
            // are forgotten when the object changes zones (CR 400.7). The cast X is captured as
            // last-known info on the leave ZoneChangeEvent so dies/leaves triggers can still read it.
            .without<com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent>()
            .without<com.wingedsheep.engine.state.components.battlefield.CastForImpendingComponent>()
            .without<com.wingedsheep.engine.state.components.battlefield.SuspendedComponent>()
            .without<com.wingedsheep.engine.state.components.battlefield.ParadigmComponent>()
            // Note: CastRecordComponent is NOT stripped here — it needs to persist
            // for intervening-if checks on mana-spent-gated triggers that may still
            // be on the stack when the permanent leaves the battlefield (e.g., evoke).
            .without<DamageComponent>()
            .without<DamageDealtToCreaturesThisTurnComponent>()
            .without<WasDealtDamageThisTurnComponent>()
            .without<HasDealtDamageComponent>()
            // A permanent that leaves and returns is a new object (CR 400.7) that has never become
            // tapped, so its "first time tapped this turn" window starts over.
            .without<HasBecomeTappedComponent>()
            .without<HasDealtCombatDamageToPlayerComponent>()
            .without<CountersComponent>()
            // A battle's protector is a designation on the battlefield object (CR 310.9); the
            // object that leaves takes it with it, and one that comes back is a new object
            // (CR 400.7) whose protector is chosen afresh by the CR 704.5x state-based action.
            .without<com.wingedsheep.engine.state.components.battlefield.ProtectorComponent>()
            // "Activate only once" memory (CR 702.177 Exhaust, and any `ActivationRestriction.Once`
            // ability) is tracked per object. A permanent that leaves and re-enters the battlefield
            // is a new object with no memory (CR 400.7 / 403.4), so the once-ever record must reset —
            // otherwise a blinked/bounced-and-recast exhaust permanent could never use its ability
            // again. Mirrors the documented intent of the sibling "ever" trackers.
            .without<AbilityActivatedEverComponent>()
            .without<AttachedToComponent>()
            .without<AttachmentsComponent>()
            // A creature that leaves the battlefield becomes unpaired (CR 702.95e), and the object
            // that comes back is a new one (CR 400.7) that must not inherit a stale partner id.
            // This strips the *leaving* half; SoulbondPairingCheck unpairs the half left behind.
            .without<com.wingedsheep.engine.state.components.battlefield.PairedComponent>()
            // A blink returns a new object (CR 400.7); it must not carry a stale "host left" marker
            // from a prior attachment, and an Equipment that itself re-enters starts unmarked.
            .without<AttachmentHostLeftComponent>()
            .without<EnteredThisTurnComponent>()
            .without<ExileOnLeaveBattlefieldComponent>()
            .without<com.wingedsheep.engine.state.components.battlefield.EnteredViaAbilityComponent>()
            .without<SagaComponent>()
            .without<com.wingedsheep.engine.state.components.battlefield.SolvedComponent>()
            .without<ReplacementEffectSourceComponent>()
            .without<TimestampComponent>()
            .without<com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent>()
            // Rule 400.7: zone changes create a new object with no memory of prior existence.
            // Strip linked exile tracking so the new instance starts with no exiled cards.
            .without<LinkedExileComponent>()
            .without<com.wingedsheep.engine.state.components.battlefield.NotedExileComponent>()
            .without<MayCastFromLinkedExileUsedThisTurnComponent>()
            // Craft materials linked to the prior battlefield existence (CR 702.167c) — the
            // re-entering object is a new object, so it has no recorded materials. The
            // craft-return executor explicitly re-attaches this component on its own entry path.
            .without<CraftedFromExiledComponent>()
            // Combat
            .without<AttackingComponent>()
            .without<BlockingComponent>()
            .without<BlockedComponent>()
            .without<DamageAssignmentComponent>()
            .without<DamageAssignmentOrderComponent>()
            .without<AttackerOrderComponent>()
            .without<DealtFirstStrikeDamageComponent>()
            .without<RequiresManualDamageAssignmentComponent>()
    }

    /**
     * Destroy a permanent (move to graveyard).
     *
     * @param state The current game state
     * @param entityId The entity to destroy
     * @param canRegenerate If false, regeneration shields are not checked (e.g. Wrath of God)
     * @return The execution result with updated state and events
     */
    fun destroyPermanent(state: GameState, entityId: EntityId, canRegenerate: Boolean = true): EffectResult {
        val container = state.getEntity(entityId)
            ?: return EffectResult.error(state, "Entity not found: $entityId")

        container.get<CardComponent>()
            ?: return EffectResult.error(state, "Not a card: $entityId")

        // Check for indestructible - indestructible permanents can't be destroyed
        if (state.projectedState.hasKeyword(entityId, Keyword.INDESTRUCTIBLE)) {
            return EffectResult.success(state)
        }

        // CR 122.1c: "If this permanent would be destroyed as the result of an effect, instead
        // remove a shield counter from it." This is that destruction-by-effect chokepoint; the
        // lethal-damage state-based action deliberately does not consult shield counters (see
        // [consumeShieldCounter]).
        //
        // Checked before regeneration because CR 616.1 lets the permanent's controller order the
        // applicable replacement effects, and the shield counter is strictly the better one to spend
        // first: it costs no tap, doesn't remove the permanent from combat, doesn't clear marked
        // damage, survives "can't be regenerated", and leaves any regeneration shield banked for
        // later. It is *not* regeneration (per the official rulings) — hence its own branch rather
        // than a synthesized regeneration shield.
        consumeShieldCounter(state, entityId)?.let { (shieldedState, event) ->
            return EffectResult.success(shieldedState, listOf(event))
        }

        // Check for regeneration shields
        if (canRegenerate) {
            val (shieldState, wasRegenerated) = applyRegenerationShields(state, entityId)
            if (wasRegenerated) {
                return applyRegenerationReplacement(shieldState, entityId)
            }
        }

        // Check for remove-damage destruction shields (Pyramids). Independent of
        // `canRegenerate` — Pyramids' replacement isn't a regeneration ability and isn't
        // shut off by "can't be regenerated this turn" effects.
        val (damageShieldState, wasShielded) = applyRemoveDamageShields(state, entityId)
        if (wasShielded) {
            return applyRemoveDamageReplacement(damageShieldState, entityId)
        }

        // Delegate to ZoneTransitionService
        val result = ZoneTransitionService.moveToZone(state, entityId, Zone.GRAVEYARD)
        return EffectResult.success(result.state, result.events)
    }

    /**
     * Move a card from any zone to another zone.
     * Finds the card's current zone and moves it to the destination.
     *
     * @param state The current game state
     * @param entityId The entity to move
     * @param targetZone The destination zone type
     * @return The execution result with updated state and events
     */
    fun moveCardToZone(state: GameState, entityId: EntityId, targetZone: Zone): EffectResult {
        val container = state.getEntity(entityId)
            ?: return EffectResult.error(state, "Entity not found")

        container.get<CardComponent>()
            ?: return EffectResult.error(state, "Not a card")

        val result = ZoneTransitionService.moveToZone(state, entityId, targetZone)
        return EffectResult.success(result.state, result.events)
    }

    /**
     * Check if a zone change to graveyard should be redirected to another zone
     * by a RedirectZoneChange replacement effect on the battlefield.
     *
     * For example, Anafenza, the Foremost exiles opponent's nontoken creature cards
     * instead of letting them go to the graveyard.
     *
     * @param state The current game state
     * @param entityId The entity about to change zones
     * @param fromZone The zone the entity is coming from (null if unknown)
     * @param toZone The intended destination zone
     * @return The redirect result with destination zone and any additional effects
     */
    /**
     * Whether the move currently in flight matches a replacement's [ZoneChangeCause] qualifier.
     *
     * [ZoneChangeCause.Any] is always satisfied. `DiscardedByOpponentEffect` reads the transient
     * [GameState.pendingDiscardCauseControllers] marker stamped by the discard sites and requires
     * both that a spell/ability caused this discard at all and that its controller is an opponent of
     * the discarding player (the card's owner — you only ever discard from your own hand). That
     * rules out the cleanup-step hand-size discard, cost payments, and your own Faithless Looting.
     */
    private fun causeSatisfied(
        state: GameState,
        entityId: EntityId,
        container: ComponentContainer,
        requiredCause: ZoneChangeCause
    ): Boolean = when (requiredCause) {
        ZoneChangeCause.Any -> true
        ZoneChangeCause.DiscardedByOpponentEffect -> {
            val causeControllerId = state.pendingDiscardCauseControllers[entityId]
            val discardingPlayerId = container.get<CardComponent>()?.ownerId
            causeControllerId != null &&
                discardingPlayerId != null &&
                causeControllerId != discardingPlayerId
        }
    }

    /**
     * The madness cost that applies (CR 702.35a) when *this particular move* is the discard the
     * madness replacement replaces, or null otherwise. A hand → graveyard move is a discard by
     * definition (CR 701.8a), so the from/to pair is the whole test.
     *
     * The cost may be printed on the card or granted by a battlefield permanent (Falkenrath
     * Gorger); [MadnessGrants] is the single place that decides which, so both readers here get
     * granted madness for free.
     *
     * Single home for the predicate: [checkZoneChangeRedirect] uses it to divert the move to exile,
     * and `ZoneTransitionService.moveToZone` uses the same call to recognise the diverted move
     * afterwards and stamp the exiled card. Two readers, one rule.
     */
    fun madnessDiscardExile(
        state: GameState,
        entityId: EntityId,
        container: ComponentContainer,
        fromZone: Zone?,
        toZone: Zone
    ): ManaCost? =
        if (fromZone == Zone.HAND && toZone == Zone.GRAVEYARD) {
            MadnessGrants.effectiveMadnessCost(state, entityId, container)
        } else {
            null
        }

    /**
     * Resolve the destination of a zone change after replacement effects.
     *
     * @param battlefieldSourceState The state whose battlefield is scanned for permanents that
     *        host a zone-change replacement ([ReplacementEffectSourceComponent]). Defaults to
     *        [state] and should stay that way for every event that happens on its own. It differs
     *        only for state-based actions: CR 704.3 performs all applicable SBAs *simultaneously
     *        as a single event*, so a shield permanent that is itself being destroyed in that same
     *        batch still shields the others (CR 614.1 — replacement effects apply as the event
     *        happens, and it hasn't happened yet). SBA callers therefore pass the state as it
     *        stood when the check pass began; without it, whichever creature the battlefield
     *        happened to iterate first would silently decide the outcome. The moving object's own
     *        replacements (finality counter, madness, commander, self-redirect) are still read off
     *        [state] — those travel with the card, not with a battlefield source.
     */
    fun checkZoneChangeRedirect(
        state: GameState,
        entityId: EntityId,
        fromZone: Zone?,
        toZone: Zone,
        battlefieldSourceState: GameState = state
    ): ZoneChangeRedirectResult {
        val container = state.getEntity(entityId) ?: return ZoneChangeRedirectResult(toZone)

        // Card-intrinsic "would be put into [zone] from anywhere → redirect instead" self-replacement
        // (Darksteel Colossus, Progenitus). It says "from anywhere", so it functions from every
        // zone (CR 113.6b), and it is read off
        // the moving card itself rather than by scanning the battlefield. It stops applying only while
        // the source is on the battlefield and has lost all abilities.
        val selfRedirect = container
            .get<com.wingedsheep.engine.state.components.identity.SelfZoneRedirectComponent>()
        if (selfRedirect != null &&
            !(fromZone == Zone.BATTLEFIELD && state.projectedState.hasLostAllAbilities(entityId))
        ) {
            for (effect in selfRedirect.redirects) {
                val event = effect.appliesTo
                if (event !is com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent) continue
                if (event.to != null && event.to != toZone) continue
                if (event.from != null && event.from != fromZone) continue
                if (!causeSatisfied(state, entityId, container, effect.requiredCause)) continue
                return ZoneChangeRedirectResult(
                    effect.newDestination,
                    shuffleIntoLibrary = effect.shuffleIntoLibrary,
                    reveal = effect.reveal
                )
            }
        }

        // Madness (CR 702.35a) — "if a player would discard this card, that player discards it, but
        // exiles it instead of putting it into their graveyard." Card-intrinsic like the
        // self-redirect above (it functions from hand), and unqualified by cause: it applies to an
        // opponent's Mind Rot, a cycling cost, and the cleanup-step hand-size discard alike.
        if (madnessDiscardExile(state, entityId, container, fromZone, toZone) != null) {
            return ZoneChangeRedirectResult(Zone.EXILE)
        }

        // Check if the entity itself has ExileOnLeaveBattlefieldComponent
        // (e.g., creature returned by Kheru Lich Lord, Whip of Erebos)
        if (fromZone == Zone.BATTLEFIELD && toZone != Zone.EXILE &&
            container.has<ExileOnLeaveBattlefieldComponent>()
        ) {
            return ZoneChangeRedirectResult(Zone.EXILE)
        }

        // Commander zone-change shortcut (CR 903.9). When `alwaysDivertToCommand` is enabled
        // on a Commander-enabled format, a card with CommanderComponent that would move to
        // graveyard / exile / hand / library from any other zone is silently diverted to the
        // command zone. Token copies of a commander aren't the commander itself (CR 903.10a)
        // and never carry CommanderComponent, so the TokenComponent guard is implicit.
        //
        // The default path leaves the destination unchanged — the commander reaches the
        // intended zone and the CR 903.9a state-based action (see CommanderZoneChoiceCheck)
        // prompts the owner before priority is granted.
        if (container.has<CommanderComponent>() &&
            toZone in COMMANDER_DIVERT_DESTINATIONS &&
            fromZone != Zone.COMMAND
        ) {
            val format = state.format
            if (format.usesCommanders && format.alwaysDivertToCommand) {
                return ZoneChangeRedirectResult(Zone.COMMAND)
            }
        }

        // Check for finality counter — if a permanent with a finality counter
        // would die (go from battlefield to graveyard), exile it instead.
        if (fromZone == Zone.BATTLEFIELD && toZone == Zone.GRAVEYARD) {
            val counters = container.get<CountersComponent>()
            if (counters != null && counters.getCount(CounterType.FINALITY) > 0) {
                return ZoneChangeRedirectResult(Zone.EXILE)
            }
        }

        for (permanentId in battlefieldSourceState.getBattlefield()) {
            val permContainer = battlefieldSourceState.getEntity(permanentId) ?: continue
            val replacementComponent = permContainer.get<ReplacementEffectSourceComponent>() ?: continue
            val sourceControllerId = permContainer.get<ControllerComponent>()?.playerId ?: continue

            for (effect in replacementComponent.replacementEffects) {
                when (effect) {
                    is RedirectZoneChange -> {
                        // selfOnly redirects are the moving card's own ability and are handled
                        // above via SelfZoneRedirectComponent (in every zone); never let them
                        // redirect other cards passing through the battlefield source.
                        if (effect.selfOnly) continue

                        val event = effect.appliesTo
                        if (event !is com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent) continue

                        // Check destination zone matches
                        if (event.to != null && event.to != toZone) continue

                        // Check source zone matches (if specified)
                        if (event.from != null && event.from != fromZone) continue

                        // Check filter against the entity being moved
                        if (!matchesZoneChangeFilter(state, entityId, container, event.filter, sourceControllerId)) continue

                        // Honour any cause qualifier (e.g. "only when discarded by an opponent's
                        // spell or ability") the same way the self-replacement path above does.
                        if (!causeSatisfied(state, entityId, container, effect.requiredCause)) continue

                        // Match found — redirect to new destination. When the replacement links
                        // its exiled cards to the source (Valgavoth), carry the source id so the
                        // mover can attach the card to its LinkedExileComponent after the move.
                        val linkSource = if (effect.linkToSource && effect.newDestination == Zone.EXILE) permanentId else null
                        return ZoneChangeRedirectResult(effect.newDestination, linkSourceId = linkSource)
                    }
                    is RedirectZoneChangeWithEffect -> {
                        // selfOnly: only applies when the entity being moved IS this permanent
                        if (effect.selfOnly && permanentId != entityId) continue

                        val event = effect.appliesTo
                        if (event !is com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent) continue

                        if (event.to != null && event.to != toZone) continue
                        if (event.from != null && event.from != fromZone) continue
                        if (!effect.selfOnly && !matchesZoneChangeFilter(state, entityId, container, event.filter, sourceControllerId)) continue

                        // Match found — redirect AND return additional effect. When the replacement
                        // links its exiled cards to the source (The Darkness Crystal), carry the
                        // source id so the mover attaches the card to its LinkedExileComponent.
                        val linkSource = if (effect.linkToSource && effect.newDestination == Zone.EXILE) permanentId else null
                        return ZoneChangeRedirectResult(
                            effect.newDestination,
                            effect.additionalEffect,
                            sourceControllerId,
                            effectSourceId = permanentId,
                            linkSourceId = linkSource
                        )
                    }
                    else -> continue
                }
            }
        }

        // Granted (durational) replacement effects — e.g. Forgotten Cellar's "if a card would be
        // put into your graveyard from anywhere this turn, exile it instead". Recorded in
        // GameState.grantedReplacementEffects with the granting controller, and read here
        // alongside permanents' printed replacement effects.
        for (grant in state.grantedReplacementEffects) {
            val effect = grant.replacement
            if (effect !is RedirectZoneChange) continue

            val event = effect.appliesTo
            if (event !is com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent) continue

            if (event.to != null && event.to != toZone) continue
            if (event.from != null && event.from != fromZone) continue
            if (!matchesZoneChangeFilter(state, entityId, container, event.filter, grant.controllerId)) continue
            if (!causeSatisfied(state, entityId, container, effect.requiredCause)) continue

            return ZoneChangeRedirectResult(effect.newDestination)
        }

        return ZoneChangeRedirectResult(toZone)
    }

    /**
     * Check if an entity matches a GameObjectFilter for zone change replacement effects.
     * Uses base state (not projected) since the entity may be leaving the battlefield.
     */
    private fun matchesZoneChangeFilter(
        state: GameState,
        entityId: EntityId,
        container: ComponentContainer,
        filter: GameObjectFilter,
        sourceControllerId: EntityId
    ): Boolean {
        if (filter == GameObjectFilter.Any) return true

        val cardComponent = container.get<CardComponent>() ?: return false

        // Check card predicates
        for (predicate in filter.cardPredicates) {
            val matches = when (predicate) {
                CardPredicate.IsCreature -> cardComponent.typeLine.isCreature
                CardPredicate.IsNontoken -> !container.has<TokenComponent>()
                CardPredicate.IsToken -> container.has<TokenComponent>()
                CardPredicate.IsLand -> cardComponent.typeLine.isLand
                CardPredicate.IsArtifact -> cardComponent.typeLine.isArtifact
                CardPredicate.IsEnchantment -> cardComponent.typeLine.isEnchantment
                CardPredicate.IsNonland -> !cardComponent.typeLine.isLand
                CardPredicate.IsNoncreature -> !cardComponent.typeLine.isCreature
                CardPredicate.IsNonenchantment -> !cardComponent.typeLine.isEnchantment
                CardPredicate.IsNonartifact -> !cardComponent.typeLine.isArtifact
                CardPredicate.IsPermanent -> cardComponent.typeLine.isPermanent
                CardPredicate.IsLegendary -> cardComponent.typeLine.isLegendary
                CardPredicate.IsNonlegendary -> !cardComponent.typeLine.isLegendary
                CardPredicate.IsDoubleFaced -> cardComponent.isDoubleFaced
                else -> true // For unhandled predicates, don't filter out
            }
            if (!matches) return false
        }

        // Check controller/owner predicate
        val controllerPredicate = filter.controllerPredicate ?: return true
        return controllerPredicate.evaluateWith { leaf ->
            when (leaf) {
                ControllerPredicate.OwnedByOpponent -> {
                    val ownerId = cardComponent.ownerId
                    ownerId != null && ownerId != sourceControllerId
                }
                ControllerPredicate.OwnedByYou -> {
                    cardComponent.ownerId == sourceControllerId
                }
                ControllerPredicate.ControlledByYou -> {
                    val controllerId = container.get<ControllerComponent>()?.playerId
                    controllerId == sourceControllerId
                }
                ControllerPredicate.ControlledByOpponent -> {
                    val controllerId = container.get<ControllerComponent>()?.playerId
                    controllerId != null && controllerId != sourceControllerId
                }
                else -> null // leaf kinds this site can't evaluate don't constrain
            }
        }
    }

    /**
     * Apply the additional effect from a RedirectZoneChangeWithEffect replacement.
     * Supports TakeExtraTurnEffect (Ugin's Nexus), AddCountersEffect (Darigaaz Reincarnated),
     * and GainLifeEffect (The Darkness Crystal — "instead exile it and you gain 2 life").
     *
     * Returns the updated state plus any events the effect emitted (e.g. LifeGainedEvent, so
     * "whenever you gain life" triggers see the rider). Callers must fold the events into their
     * own event stream.
     *
     * @param entityId The entity that was redirected (for effects that target it, like adding counters)
     */
    /**
     * Execute the rider a zone-change replacement carries — the "When you do, …" half of
     * "…exile it instead. When you do, create a 2/2 green Wolf creature token".
     *
     * @param controllerId Controller of the replacement's source; the rider's "you".
     * @param entityId The card that was redirected, for riders that act on it (Darigaaz's egg
     *   counters).
     * @param sourceId The permanent hosting the replacement, used as the rider's source. Only
     *   token art reads it today; it may already have left the battlefield (CR 704.3), which is
     *   fine — nothing here requires the source to still be there.
     */
    fun applyReplacementAdditionalEffect(
        state: GameState,
        effect: com.wingedsheep.sdk.scripting.effects.Effect,
        controllerId: EntityId?,
        entityId: EntityId? = null,
        sourceId: EntityId? = null
    ): Pair<GameState, List<EngineGameEvent>> {
        if (effect is com.wingedsheep.sdk.scripting.effects.TakeExtraTurnEffect) {
            // Check if extra turns are prevented by any permanent on the battlefield
            if (ReplacementEffectUtils.isExtraTurnPrevented(state)) return state to emptyList()

            val cid = controllerId ?: return state to emptyList()
            val newState = state.getOpponents(cid).fold(state) { acc, opponentId ->
                acc.updateEntity(opponentId) { container ->
                    val existing = container.get<SkipNextTurnComponent>()?.turns ?: 0
                    container.with(SkipNextTurnComponent(existing + 1))
                }
            }
            return newState to emptyList()
        }
        if (effect is com.wingedsheep.sdk.scripting.effects.AddCountersEffect && entityId != null) {
            val counterType = try {
                com.wingedsheep.sdk.core.CounterType.valueOf(
                    effect.counterType.uppercase()
                        .replace(' ', '_')
                        .replace('+', 'P')
                        .replace('-', 'M')
                        .replace("/", "_")
                )
            } catch (_: IllegalArgumentException) {
                com.wingedsheep.sdk.core.CounterType.PLUS_ONE_PLUS_ONE
            }
            val current = state.getEntity(entityId)?.get<CountersComponent>() ?: CountersComponent()
            val newState = state.updateEntity(entityId) { container ->
                container.with(current.withAdded(counterType, effect.count))
            }
            return newState to emptyList()
        }
        if (effect is com.wingedsheep.sdk.scripting.effects.GainLifeEffect) {
            // The rider's controller (the replacement's source controller) gains the life. Only a
            // fixed amount is meaningful here — there is no full effect context at replacement time.
            val cid = controllerId ?: return state to emptyList()
            val amount = (effect.amount as? com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed)?.amount
                ?: return state to emptyList()
            val (newState, event) = DamageUtils.gainLife(state, cid, amount)
            return newState to listOfNotNull(event)
        }
        if (effect is com.wingedsheep.sdk.scripting.effects.CreateTokenEffect) {
            // Head of the Hunt. Delegated to the real executor rather than open-coded: that is
            // what gives the token the minting set's art, its keywords and static abilities, and
            // the enters-the-battlefield events that ETB triggers read. Its controller is the
            // replacement's controller — the rider's "you" — so a token minted while an opponent's
            // creature is redirected still lands on the shield controller's side.
            val cid = controllerId ?: return state to emptyList()
            val result = tokenExecutor.execute(
                state,
                effect,
                com.wingedsheep.engine.handlers.EffectContext(sourceId = sourceId, controllerId = cid)
            )
            if (result.error != null) return state to emptyList()
            return result.state to result.events
        }
        return state to emptyList()
    }

    /**
     * Check for regeneration shields on an entity and consume one if found.
     *
     * Regeneration shields are floating effects with RegenerationShield modification.
     * If a CantBeRegenerated floating effect targets the entity, regeneration is blocked.
     *
     * @param state The current game state
     * @param entityId The entity being destroyed
     * @return Pair of (updated state with consumed shield, whether regeneration occurred)
     */
    fun applyRegenerationShields(state: GameState, entityId: EntityId): Pair<GameState, Boolean> {
        // Check if the entity has a CantBeRegenerated floating effect
        val cantRegenerate = state.floatingEffects.any { effect ->
            effect.effect.modification is SerializableModification.CantBeRegenerated &&
                entityId in effect.effect.affectedEntities
        }
        if (cantRegenerate) return state to false

        // Find the first regeneration shield targeting this entity
        val shieldIndex = state.floatingEffects.indexOfFirst { effect ->
            effect.effect.modification is SerializableModification.RegenerationShield &&
                entityId in effect.effect.affectedEntities
        }
        if (shieldIndex == -1) return state to false

        // Consume the shield (remove it)
        val updatedEffects = state.floatingEffects.toMutableList()
        updatedEffects.removeAt(shieldIndex)

        return state.copy(floatingEffects = updatedEffects) to true
    }

    /**
     * Apply regeneration replacement effect: tap the creature, remove all damage,
     * and remove it from combat. The creature stays on the battlefield.
     *
     * The tap runs through [com.wingedsheep.engine.core.tap], so callers must fold the returned
     * events into their own result or "becomes tapped" triggers silently go missing.
     *
     * @param state The current game state (shield already consumed)
     * @param entityId The entity being regenerated
     * @return ExecutionResult with the regenerated creature still on the battlefield, carrying the
     *   [com.wingedsheep.engine.core.TappedEvent] when the creature was untapped
     */
    fun applyRegenerationReplacement(state: GameState, entityId: EntityId): EffectResult {
        val entity = state.getEntity(entityId)
            ?: return EffectResult.success(state)

        val isAttacking = entity.has<AttackingComponent>()
        val isBlocking = entity.has<BlockingComponent>()

        // "Remove all damage marked on it" (CR 701.19a) is the CR 701.69a heal action — one shared
        // primitive so every damage-removal site behaves identically.
        val healedState = DamageUtils.healMarkedDamage(state, entityId)

        // CR 701.19a: "…instead remove all damage marked on it and its controller taps it. If it's
        // an attacking or blocking creature, remove it from combat." The tap is a real tap
        // transition, so it goes through the `tap()` atom rather than being open-coded: that emits
        // the [TappedEvent] "becomes tapped" triggers read (Captain America, Living Legend;
        // Deeproot Pilgrimage) and writes the `HasBecomeTappedComponent` turn stamp, so a later tap
        // that turn is correctly *not* the creature's first. `tap()` attributes the tap to the
        // permanent's controller by default, which is what "its controller taps it" asks for.
        //
        // Regenerating an *already tapped* creature is not a transition — CR 701.26a, only untapped
        // permanents can be tapped — so `tap()` no-ops and emits nothing. The rest of the
        // replacement (damage removal, combat removal) still applies, which is why the tap is taken
        // separately from the component strip below rather than folded into it.
        val (tappedState, tappedEvent) = tap(healedState, entityId)

        // Strip combat components from the regenerated creature
        var newState = tappedState.updateEntity(entityId) { c ->
            c.without<AttackingComponent>()
                .without<BlockingComponent>()
                .without<BlockedComponent>()
                .without<DamageAssignmentComponent>()
                .without<DamageAssignmentOrderComponent>()
                .without<AttackerOrderComponent>()
        }

        // Clean up cross-references in other creatures' combat components
        // (same logic as RemoveFromCombatExecutor)

        // If the regenerated creature was an attacker, remove it from blockers' BlockingComponent
        if (isAttacking) {
            for ((otherId, components) in newState.entities) {
                val blockingComponent = components.get<BlockingComponent>() ?: continue
                if (entityId in blockingComponent.blockedAttackerIds) {
                    val updatedIds = blockingComponent.blockedAttackerIds - entityId
                    newState = if (updatedIds.isEmpty()) {
                        newState.updateEntity(otherId) { c -> c.without<BlockingComponent>() }
                    } else {
                        newState.updateEntity(otherId) { c -> c.with(BlockingComponent(updatedIds)) }
                    }
                }
            }
        }

        // If the regenerated creature was a blocker, remove it from attackers' BlockedComponent.
        // Keep BlockedComponent even if empty — a blocked creature stays blocked per MTG rules
        // (it just deals no combat damage without trample).
        if (isBlocking) {
            val blockedAttackerIds = entity.get<BlockingComponent>()?.blockedAttackerIds ?: emptyList()
            for (attackerId in blockedAttackerIds) {
                val attackerEntity = newState.getEntity(attackerId) ?: continue
                val blockedComponent = attackerEntity.get<BlockedComponent>() ?: continue
                val updatedBlockerIds = blockedComponent.blockerIds - entityId
                newState = newState.updateEntity(attackerId) { c ->
                    c.with(BlockedComponent(updatedBlockerIds))
                }
            }
        }

        return EffectResult.success(newState, listOfNotNull(tappedEvent))
    }

    /**
     * Check for remove-damage destruction shields (Pyramids) on an entity and consume
     * one if found. Stored as floating effects with the `RemoveDamageShield`
     * modification.
     *
     * @return Pair of (updated state with consumed shield, whether a shield fired)
     */
    fun applyRemoveDamageShields(state: GameState, entityId: EntityId): Pair<GameState, Boolean> {
        val shieldIndex = state.floatingEffects.indexOfFirst { effect ->
            effect.effect.modification is SerializableModification.RemoveDamageShield &&
                entityId in effect.effect.affectedEntities
        }
        if (shieldIndex == -1) return state to false

        val updatedEffects = state.floatingEffects.toMutableList()
        updatedEffects.removeAt(shieldIndex)
        return state.copy(floatingEffects = updatedEffects) to true
    }

    /**
     * Apply the remove-damage destruction replacement: strip the entity's
     * `DamageComponent`. Unlike regeneration, the permanent is NOT tapped and is
     * NOT removed from combat — Pyramids' oracle text replaces destruction only
     * with "remove all damage marked on it".
     */
    fun applyRemoveDamageReplacement(state: GameState, entityId: EntityId): EffectResult {
        return EffectResult.success(DamageUtils.healMarkedDamage(state, entityId))
    }

    /**
     * Move a permanent from battlefield to another zone, cleaning up permanent-only components.
     *
     * @param state The current game state
     * @param entityId The entity to move
     * @param targetZone The destination zone type
     * @return The execution result with updated state and events
     */
    fun movePermanentToZone(state: GameState, entityId: EntityId, targetZone: Zone): EffectResult {
        val container = state.getEntity(entityId)
            ?: return EffectResult.error(state, "Entity not found")

        container.get<CardComponent>()
            ?: return EffectResult.error(state, "Not a card")

        val result = ZoneTransitionService.moveToZone(state, entityId, targetZone)
        return EffectResult.success(result.state, result.events)
    }
}

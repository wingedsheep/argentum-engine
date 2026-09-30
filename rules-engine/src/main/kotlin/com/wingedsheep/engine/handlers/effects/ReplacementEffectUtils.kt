package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.components.player.CountersLockedThisTurnComponent
import com.wingedsheep.sdk.scripting.CapCounterPlacementThisTurn
import com.wingedsheep.sdk.scripting.DoubleCounterPlacement
import com.wingedsheep.sdk.scripting.ModifyCounterPlacement
import com.wingedsheep.sdk.scripting.PreventExtraTurns
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Utility functions for applying replacement effects that modify game actions
 * before they produce events (counter placement modifiers, extra turn prevention).
 */
object ReplacementEffectUtils {

    /**
     * Check if extra turns are prevented by any PreventExtraTurns replacement effect
     * on the battlefield (e.g., Ugin's Nexus).
     */
    fun isExtraTurnPrevented(state: GameState): Boolean {
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            for (effect in replacementComponent.replacementEffects) {
                if (effect is PreventExtraTurns) return true
            }
        }
        return false
    }

    /**
     * Apply ModifyCounterPlacement replacement effects (Hardened Scales, Winding Constrictor).
     *
     * Scans all battlefield entities for ReplacementEffectSourceComponent containing
     * ModifyCounterPlacement effects. If the counter type and recipient match, modifies
     * the counter count by the effect's modifier.
     *
     * @param state The current game state
     * @param targetId The entity receiving counters
     * @param counterType The type of counter being placed (as CounterType enum)
     * @param count The original number of counters
     * @param placerId The player placing the counters — used to gate effects that only
     *                 apply when "you" are the one placing them (e.g., Innkeeper's Talent
     *                 Level 3). When null, placer-gated effects do not apply.
     * @return The modified counter count
     */
    fun applyCounterPlacementModifiers(
        state: GameState,
        targetId: EntityId,
        counterType: CounterType,
        count: Int,
        placerId: EntityId? = null,
        predicateEvaluator: PredicateEvaluator
    ): Int {
        if (count <= 0) return count
        // A capped recipient can't get more of this kind this turn: the placement doesn't happen.
        if (isCounterPlacementLocked(state, targetId, counterType)) return 0

        var modifiedCount = count

        // 1. Static battlefield replacement effects (Hardened Scales, Doubling Season, …).
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            val sourceControllerId = container.get<ControllerComponent>()?.playerId ?: continue

            for (effect in replacementComponent.replacementEffects) {
                val counterEvent = when (effect) {
                    is ModifyCounterPlacement -> effect.appliesTo
                    is DoubleCounterPlacement -> effect.appliesTo
                    else -> continue
                }
                if (counterEvent !is com.wingedsheep.sdk.scripting.EventPattern.CounterPlacementEvent) continue

                // Gate on "If YOU would put..." — skip when an opponent is the placer.
                val placedByYouOnly = when (effect) {
                    is ModifyCounterPlacement -> effect.placedByYou
                    is DoubleCounterPlacement -> effect.placedByYou
                }
                if (placedByYouOnly && placerId != sourceControllerId) continue

                if (counterEvent.counterType != null && counterEvent.counterType != counterType) continue

                // Check recipient filter
                val recipientMatches = matchesRecipient(
                    counterEvent.recipient, state, targetId, entityId, sourceControllerId,
                    predicateEvaluator = predicateEvaluator
                )
                if (!recipientMatches) continue

                when (effect) {
                    is ModifyCounterPlacement -> modifiedCount += effect.modifier
                    is DoubleCounterPlacement -> modifiedCount *= 2
                }
            }
        }

        // 2. Temporary, duration-scoped, controller-scoped modifiers
        //    (GrantCounterPlacementModifierEffect — e.g. Prairie Dog's {4}{W}).
        //    These have no battlefield source permanent: the effect's stored controllerId IS
        //    the "you" for both the recipient filter ("a creature you control") and the placer
        //    gate (only the controller's own counter placements get the bonus).
        for (modifier in state.activeCounterPlacementModifiers) {
            if (placerId != modifier.controllerId) continue
            if (modifier.counterType != counterType) continue
            // No battlefield source entity — pass the controller as the "source entity" so
            // Recipient.Self can't spuriously match, and the controller as controllerId.
            val recipientMatches = matchesRecipient(
                modifier.recipient, state, targetId, modifier.controllerId, modifier.controllerId,
                predicateEvaluator = predicateEvaluator
            )
            if (!recipientMatches) continue
            modifiedCount += modifier.modifier
        }

        // 3. Caps apply last — the order the affected player would pick (Melira's ruling), so a
        //    doubler or an additive modifier can't push a capped placement past the cap.
        val cap = counterPlacementCap(state, targetId, counterType, predicateEvaluator)
        if (cap != null) modifiedCount = minOf(modifiedCount, cap)

        return modifiedCount.coerceAtLeast(0)
    }

    /** Whether [targetId] already took a [CapCounterPlacementThisTurn] lock on [counterType] this turn. */
    fun isCounterPlacementLocked(state: GameState, targetId: EntityId, counterType: CounterType): Boolean =
        state.getEntity(targetId)?.get<CountersLockedThisTurnComponent>()?.kinds?.contains(counterType) == true

    /**
     * Record the lock a [CapCounterPlacementThisTurn] replacement leaves behind — "and you can't
     * get additional poison counters this turn". Call it after placing [placed] counters of
     * [counterType] on a player; it stamps the lock only when a cap actually applied to that
     * placement. The lock outlives the cap's source (it's the replacement's result), so it lives on
     * the player and is cleared at end of turn.
     */
    fun recordCounterPlacementLock(
        state: GameState,
        targetId: EntityId,
        counterType: CounterType,
        placed: Int,
        predicateEvaluator: PredicateEvaluator
    ): GameState {
        if (placed <= 0 || targetId !in state.turnOrder) return state
        counterPlacementCap(state, targetId, counterType, predicateEvaluator) ?: return state
        return state.updateEntity(targetId) { container ->
            val existing = container.get<CountersLockedThisTurnComponent>() ?: CountersLockedThisTurnComponent()
            container.with(existing.with(counterType))
        }
    }

    /** The tightest [CapCounterPlacementThisTurn] on the battlefield that covers this placement, or null. */
    private fun counterPlacementCap(
        state: GameState,
        targetId: EntityId,
        counterType: CounterType,
        predicateEvaluator: PredicateEvaluator
    ): Int? {
        var cap: Int? = null
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            // "You" is the source's current (projected) controller — a stolen Melira protects its thief.
            val sourceControllerId = state.projectedState.getController(entityId) ?: continue
            for (effect in replacementComponent.replacementEffects) {
                if (effect !is CapCounterPlacementThisTurn) continue
                val counterEvent = effect.appliesTo as? com.wingedsheep.sdk.scripting.EventPattern.CounterPlacementEvent
                    ?: continue
                if (counterEvent.counterType != null && counterEvent.counterType != counterType) continue
                if (!matchesRecipient(
                        counterEvent.recipient, state, targetId, entityId, sourceControllerId,
                        predicateEvaluator = predicateEvaluator
                    )
                ) continue
                cap = minOf(cap ?: effect.amount, effect.amount)
            }
        }
        return cap
    }

    /**
     * The counter-placement side of [PredicateEvaluator.matchesRecipient]. A creature still on its
     * way onto the battlefield (entering with counters, CR 614.12) has no projection entry yet, so
     * the filter reads its own characteristics — as it would exist on the battlefield.
     */
    private fun matchesRecipient(
        recipient: Recipient,
        state: GameState,
        targetId: EntityId,
        sourceEntityId: EntityId,
        sourceControllerId: EntityId,
        predicateEvaluator: PredicateEvaluator
    ): Boolean = predicateEvaluator.matchesRecipient(
        state, state.projectedState, targetId, recipient,
        PredicateContext(controllerId = sourceControllerId, sourceId = sourceEntityId),
    )
}

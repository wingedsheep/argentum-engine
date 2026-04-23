package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.DoubleCounterPlacement
import com.wingedsheep.sdk.scripting.ModifyCounterPlacement
import com.wingedsheep.sdk.scripting.PreventExtraTurns
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.events.RecipientFilter

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
        placerId: EntityId? = null
    ): Int {
        if (count <= 0) return count

        var modifiedCount = count

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
                if (counterEvent !is com.wingedsheep.sdk.scripting.GameEvent.CounterPlacementEvent) continue

                // Gate on "If YOU would put..." — skip when an opponent is the placer.
                if (effect is DoubleCounterPlacement && effect.placedByYou && placerId != sourceControllerId) continue

                // Check counter type filter
                val counterTypeMatches = when (counterEvent.counterType) {
                    is CounterTypeFilter.Any -> true
                    is CounterTypeFilter.PlusOnePlusOne -> counterType == CounterType.PLUS_ONE_PLUS_ONE
                    is CounterTypeFilter.MinusOneMinusOne -> counterType == CounterType.MINUS_ONE_MINUS_ONE
                    is CounterTypeFilter.Loyalty -> counterType == CounterType.LOYALTY
                    is CounterTypeFilter.Named -> {
                        try {
                            val namedType = CounterType.valueOf(
                                (counterEvent.counterType as CounterTypeFilter.Named).name.uppercase().replace(' ', '_')
                            )
                            counterType == namedType
                        } catch (_: IllegalArgumentException) {
                            false
                        }
                    }
                }
                if (!counterTypeMatches) continue

                // Check recipient filter
                val recipientMatches = matchesRecipientFilter(
                    counterEvent.recipient, state, targetId, entityId, sourceControllerId
                )
                if (!recipientMatches) continue

                when (effect) {
                    is ModifyCounterPlacement -> modifiedCount += effect.modifier
                    is DoubleCounterPlacement -> modifiedCount *= 2
                }
            }
        }

        return modifiedCount.coerceAtLeast(0)
    }

    private fun matchesRecipientFilter(
        recipient: RecipientFilter,
        state: GameState,
        targetId: EntityId,
        sourceEntityId: EntityId,
        sourceControllerId: EntityId
    ): Boolean = when (recipient) {
        is RecipientFilter.CreatureYouControl -> {
            val isCreature = state.getEntity(targetId)?.get<CardComponent>()?.typeLine?.isCreature == true
            val isControlled = state.getEntity(targetId)?.get<ControllerComponent>()?.playerId == sourceControllerId
            isCreature && isControlled
        }
        is RecipientFilter.Any -> true
        is RecipientFilter.Self -> targetId == sourceEntityId
        is RecipientFilter.PermanentYouControl -> {
            state.getEntity(targetId)?.get<ControllerComponent>()?.playerId == sourceControllerId
        }
        is RecipientFilter.You -> targetId == sourceControllerId
        else -> false
    }
}

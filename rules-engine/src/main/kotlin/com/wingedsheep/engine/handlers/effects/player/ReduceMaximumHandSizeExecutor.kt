package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.MaximumHandSizeReducedEvent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.player.PlayerMaximumHandSizeReductionComponent
import com.wingedsheep.sdk.scripting.effects.ReduceMaximumHandSizeEffect
import kotlin.reflect.KClass

/**
 * Resolves [ReduceMaximumHandSizeEffect].
 *
 * Evaluates [ReduceMaximumHandSizeEffect.amount] once at resolution (locking in the number "for the
 * rest of the game", per CR) and *accumulates* it into the target player's
 * [PlayerMaximumHandSizeReductionComponent]. Repeat applications add to the running total (two
 * Inspired Ideas reduce the maximum by six). A non-positive reduction is a no-op (no state change,
 * no event). [com.wingedsheep.engine.core.MaximumHandSize.effective] subtracts the accumulated
 * total from the computed maximum, floored at 0.
 */
class ReduceMaximumHandSizeExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<ReduceMaximumHandSizeEffect> {

    override val effectType: KClass<ReduceMaximumHandSizeEffect> = ReduceMaximumHandSizeEffect::class

    override fun execute(
        state: GameState,
        effect: ReduceMaximumHandSizeEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for reduce-maximum-hand-size")

        if (!state.turnOrder.contains(targetId)) {
            return EffectResult.error(state, "Reduce-maximum-hand-size target must be a player")
        }

        val playerContainer = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target player no longer exists")

        val reduction = amountEvaluator.evaluate(state, effect.amount, context).coerceAtLeast(0)
        if (reduction == 0) {
            return EffectResult.success(state)
        }

        val existing = playerContainer.get<PlayerMaximumHandSizeReductionComponent>()?.amount ?: 0
        val newTotal = existing + reduction

        val newState = state.updateEntity(targetId) { container ->
            container.with(PlayerMaximumHandSizeReductionComponent(newTotal))
        }

        val playerName = playerContainer.get<PlayerComponent>()?.name ?: "Player"
        val sourceName = context.sourceId?.let {
            state.getEntity(it)?.get<CardComponent>()?.name
        } ?: "Unknown"

        return EffectResult.success(
            newState,
            listOf(MaximumHandSizeReducedEvent(targetId, playerName, reduction, newTotal, sourceName))
        )
    }
}

package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.destroyPermanent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.DestroyAllEffect
import kotlin.reflect.KClass

/**
 * Executor for DestroyAllEffect.
 * Unified executor that handles all "destroy all X" patterns.
 *
 * Examples:
 * - DestroyAllEffect(Land) -> "Destroy all lands"
 * - DestroyAllEffect(Creature, noRegenerate = true) -> Wrath of God
 * - DestroyAllEffect(And(Creature, WithColor(WHITE))) -> Virtue's Ruin
 */
class DestroyAllExecutor : EffectExecutor<DestroyAllEffect> {

    override val effectType: KClass<DestroyAllEffect> = DestroyAllEffect::class

    private val predicateEvaluator = PredicateEvaluator()

    override fun execute(
        state: GameState,
        effect: DestroyAllEffect,
        context: EffectContext
    ): ExecutionResult {
        var newState = state
        val events = mutableListOf<EngineGameEvent>()
        val predicateContext = PredicateContext(controllerId = context.controllerId)

        // Note: noRegenerate flag is stored but not yet enforced.
        // Regeneration support will be added in a future update.
        // When implemented, this executor will need to prevent regeneration
        // replacement effects from being applied when noRegenerate is true.

        for (entityId in state.getBattlefield()) {
            if (!predicateEvaluator.matches(state, entityId, effect.filter.baseFilter, predicateContext)) {
                continue
            }

            val result = destroyPermanent(newState, entityId)
            newState = result.newState
            events.addAll(result.events)
        }

        return ExecutionResult.success(newState, events)
    }
}

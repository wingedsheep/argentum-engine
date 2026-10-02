package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import kotlin.reflect.KClass

/**
 * Executor for GainLifeEffect.
 * "You gain X life" or "Target player gains X life"
 */
class GainLifeExecutor(
    private val amountEvaluator: DynamicAmountEvaluator,
    private val replacementProcessor: com.wingedsheep.engine.replacement.ReplacementEffectProcessor,
    private val executeEffect: (GameState, com.wingedsheep.sdk.scripting.effects.Effect, EffectContext) -> EffectResult
) : EffectExecutor<GainLifeEffect> {

    override val effectType: KClass<GainLifeEffect> = GainLifeEffect::class

    override fun execute(
        state: GameState,
        effect: GainLifeEffect,
        context: EffectContext
    ): EffectResult {
        val playerIds = context.resolvePlayerTargets(effect.target, state)
        if (playerIds.isEmpty()) {
            return EffectResult.error(state, "No valid target for life gain")
        }

        val amount = amountEvaluator.evaluate(state, effect.amount, context)

        var newState = state
        val events = mutableListOf<EngineGameEvent>()

        for ((index, playerId) in playerIds.withIndex()) {
            val remaining = playerIds.drop(index + 1).map {
                com.wingedsheep.sdk.dsl.Effects.GainLife(amount,
                    com.wingedsheep.sdk.scripting.targets.EffectTarget.SpecificEntity(it))
            }
            val prepared = if (remaining.isEmpty()) newState else newState.pushContinuation(
                com.wingedsheep.engine.core.EffectContinuation(remaining, context))
            val result = if (LifeGainReplacements.applies(prepared, playerId, amount, amountEvaluator.predicates)) {
                LifeGainReplacements.resolve(prepared, playerId, amount, context, replacementProcessor, executeEffect)
            } else {
                val (gained, event) = DamageUtils.gainLife(prepared, playerId, amount, predicateEvaluator = amountEvaluator.predicates)
                EffectResult.success(gained, listOfNotNull(event))
            }
            events.addAll(result.events)
            if (result.outcome is com.wingedsheep.engine.core.Outcome.Paused)
                return result.copy(events = events)
            newState = if (remaining.isEmpty()) result.state else result.state.popContinuation().second
        }

        return EffectResult.success(newState, events)
    }
}

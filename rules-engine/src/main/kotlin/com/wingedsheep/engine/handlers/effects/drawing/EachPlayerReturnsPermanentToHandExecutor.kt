package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.scripting.EachPlayerReturnsPermanentToHandEffect
import com.wingedsheep.sdk.scripting.Effect
import kotlin.reflect.KClass

/**
 * Executor for [EachPlayerReturnsPermanentToHandEffect].
 *
 * Delegates to [EffectPatterns.eachPlayerReturnsPermanentToHand] via the injected
 * effect executor, which runs the ForEachPlayer pipeline (gather controlled permanents,
 * select one, move to hand).
 */
class EachPlayerReturnsPermanentToHandExecutor(
    private val effectExecutor: ((GameState, Effect, EffectContext) -> ExecutionResult)? = null
) : EffectExecutor<EachPlayerReturnsPermanentToHandEffect> {

    override val effectType: KClass<EachPlayerReturnsPermanentToHandEffect> =
        EachPlayerReturnsPermanentToHandEffect::class

    override fun execute(
        state: GameState,
        effect: EachPlayerReturnsPermanentToHandEffect,
        context: EffectContext
    ): ExecutionResult {
        val executor = effectExecutor
            ?: return ExecutionResult.error(state, "No effect executor available for EachPlayerReturnsPermanentToHand")

        val pipeline = EffectPatterns.eachPlayerReturnsPermanentToHand()
        return executor(state, pipeline, context)
    }
}

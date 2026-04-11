package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlin.reflect.KClass

/**
 * Executor for ConditionalOnCollectionEffect.
 *
 * Checks whether a named collection in the pipeline context is non-empty,
 * then delegates to the appropriate sub-effect.
 */
class ConditionalOnCollectionExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> ExecutionResult
) : EffectExecutor<ConditionalOnCollectionEffect> {

    override val effectType: KClass<ConditionalOnCollectionEffect> = ConditionalOnCollectionEffect::class

    override fun execute(
        state: GameState,
        effect: ConditionalOnCollectionEffect,
        context: EffectContext
    ): ExecutionResult {
        val collection = context.pipeline.storedCollections[effect.collection] ?: emptyList()

        val elseEffect = effect.ifEmpty
        return if (collection.size >= effect.minSize) {
            effectExecutor(state, effect.ifNotEmpty, context)
        } else if (elseEffect != null) {
            effectExecutor(state, elseEffect, context)
        } else {
            ExecutionResult.success(state)
        }
    }
}

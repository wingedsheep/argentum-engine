package com.wingedsheep.engine.handlers.effects.chain

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Module providing chain copy effect executors.
 *
 * Requires deferred initialization to break the circular dependency
 * with the parent EffectExecutorRegistry (the chain executor delegates
 * the inner action to the registry).
 */
class ChainExecutors : ExecutorModule {
    private lateinit var effectExecutor: (GameState, Effect, EffectContext) -> EffectResult

    private val chainCopyExecutor by lazy { ChainCopyExecutor(effectExecutor = effectExecutor) }

    fun initialize(executor: (GameState, Effect, EffectContext) -> EffectResult) {
        effectExecutor = executor
    }

    override fun executors(): List<EffectExecutor<*>> = listOf(
        chainCopyExecutor
    )
}

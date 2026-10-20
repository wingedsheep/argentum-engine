package com.wingedsheep.engine.handlers.effects.chain

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Module providing chain copy effect executors.
 *
 * The chain executor delegates the inner action to the parent EffectExecutorRegistry, whose
 * execute function the registry hands in at construction.
 */
class ChainExecutors(
    /** The registry's re-entrant entry point, for the chain copy's sub-effects. */
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult,
    private val targetFinder: TargetFinder,
    private val predicateEvaluator: PredicateEvaluator,
    private val manaSolver: ManaSolver
) : ExecutorModule {
    private val chainCopyExecutor = ChainCopyExecutor(effectExecutor = effectExecutor, targetFinder = targetFinder, predicateEvaluator = predicateEvaluator, manaSolver = manaSolver)

    override fun executors(): List<EffectExecutor<*>> = listOf(
        chainCopyExecutor
    )
}

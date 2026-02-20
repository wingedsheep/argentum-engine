package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Module providing composite effect executors.
 *
 * These executors require a reference to the parent registry's execute function
 * to handle recursive effect execution. The module uses deferred initialization
 * to break the circular dependency.
 */
class CompositeExecutors : ExecutorModule {
    private lateinit var effectExecutor: (GameState, Effect, EffectContext) -> ExecutionResult

    private val compositeEffectExecutor by lazy { CompositeEffectExecutor(effectExecutor) }
    private val conditionalEffectExecutor by lazy { ConditionalEffectExecutor(effectExecutor) }
    private val createDelayedTriggerExecutor by lazy { CreateDelayedTriggerExecutor() }
    private val forEachTargetExecutor by lazy { ForEachTargetExecutor(effectExecutor) }
    private val forEachPlayerExecutor by lazy { ForEachPlayerExecutor(effectExecutor) }
    private val mayEffectExecutor by lazy { MayEffectExecutor(effectExecutor) }
    private val mayPayManaExecutor by lazy { MayPayManaExecutor(effectExecutor) }
    private val modalEffectExecutor by lazy { ModalEffectExecutor(effectExecutor) }
    private val reflexiveTriggerEffectExecutor by lazy { ReflexiveTriggerEffectExecutor(effectExecutor) }
    private val flipCoinExecutor by lazy { FlipCoinExecutor(effectExecutor) }
    private val forEachInGroupExecutor by lazy { ForEachInGroupExecutor(effectExecutor) }
    private val repeatWhileExecutor by lazy { RepeatWhileExecutor(effectExecutor) }

    /**
     * Initialize the module with the parent registry's execute function.
     * Must be called before executors() is accessed.
     */
    fun initialize(executor: (GameState, Effect, EffectContext) -> ExecutionResult) {
        this.effectExecutor = executor
    }

    override fun executors(): List<EffectExecutor<*>> = listOf(
        compositeEffectExecutor,
        conditionalEffectExecutor,
        createDelayedTriggerExecutor,
        forEachTargetExecutor,
        forEachPlayerExecutor,
        forEachInGroupExecutor,
        mayEffectExecutor,
        mayPayManaExecutor,
        modalEffectExecutor,
        reflexiveTriggerEffectExecutor,
        flipCoinExecutor,
        repeatWhileExecutor
    )
}

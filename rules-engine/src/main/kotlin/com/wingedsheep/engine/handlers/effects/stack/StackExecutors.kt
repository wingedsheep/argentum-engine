package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing all stack-related effect executors.
 */
class StackExecutors : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        CounterSpellExecutor(),
        CounterSpellWithFilterExecutor(),
        CounterUnlessPaysExecutor()
    )
}

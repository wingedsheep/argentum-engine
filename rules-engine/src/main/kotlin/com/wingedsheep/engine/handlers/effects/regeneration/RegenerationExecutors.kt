package com.wingedsheep.engine.handlers.effects.regeneration

import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing regeneration-related effect executors.
 */
class RegenerationExecutors : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        RegenerateExecutor(),
        CantBeRegeneratedExecutor()
    )
}

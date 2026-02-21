package com.wingedsheep.engine.handlers.effects.chain

import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing chain copy effect executors.
 */
class ChainExecutors : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        ChainCopyExecutor()
    )
}

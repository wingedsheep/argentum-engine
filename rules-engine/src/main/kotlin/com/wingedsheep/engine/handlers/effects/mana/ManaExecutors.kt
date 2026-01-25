package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing all mana-related effect executors.
 */
class ManaExecutors : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        AddManaExecutor(),
        AddColorlessManaExecutor()
    )
}

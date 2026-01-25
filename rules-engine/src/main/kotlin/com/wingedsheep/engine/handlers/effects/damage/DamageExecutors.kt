package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing all damage-related effect executors.
 */
class DamageExecutors : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        DealDamageExecutor(),
        DealXDamageExecutor(),
        DealXDamageToAllExecutor(),
        DealDamageToAllCreaturesExecutor(),
        DealDamageToAllExecutor()
    )
}

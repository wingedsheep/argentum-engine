package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing all combat-related effect executors.
 */
class CombatExecutors : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        MustBeBlockedExecutor(),
        PreventDamageFromAttackingCreaturesExecutor(),
        GrantCantBeBlockedExceptByColorExecutor(),
        ReflectCombatDamageExecutor(),
        TauntExecutor()
    )
}

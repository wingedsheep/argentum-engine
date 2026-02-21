package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing all combat-related effect executors.
 */
class CombatExecutors(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        DestroyAtEndOfCombatExecutor(),
        MustBeBlockedExecutor(),
        PreventDamageFromAttackingCreaturesExecutor(),
        PreventAllCombatDamageThisTurnExecutor(),
        GrantCantBeBlockedExceptByColorExecutor(),
        ReflectCombatDamageExecutor(),
        TauntExecutor(),
        PreventNextDamageExecutor(amountEvaluator),
        CantBlockTargetCreaturesExecutor(),
        RemoveFromCombatExecutor(),
        ChooseCreatureTypeMustAttackExecutor(),
        PreventAllDamageDealtByTargetExecutor(),
        RedirectNextDamageExecutor(),
        PreventNextDamageFromChosenCreatureTypeExecutor(),
        PreventCombatDamageFromExecutor()
    )
}

package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing all combat-related effect executors.
 */
class CombatExecutors(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        MustBeBlockedExecutor(),
        ProvokeExecutor(),
        ForceBlockExecutor(),
        PreventDamageExecutor(amountEvaluator),
        GrantCantBeBlockedExceptByColorExecutor(),
        GrantCantBeBlockedExceptByExecutor(),
        ReflectCombatDamageExecutor(),
        TauntExecutor(),
        CantAttackGroupExecutor(),
        CantBlockGroupExecutor(),
        CantAttackExecutor(),
        CantBlockExecutor(),
        RemoveFromCombatExecutor(),
        SwapBlockingAssignmentsExecutor(cardRegistry),
        OpponentGuessesTopCardKindExecutor(),
        PlayerGuessesConditionExecutor(),
        MarkMustAttackThisTurnExecutor(),
        MarkMustBlockThisTurnExecutor(),
        GoadExecutor(),
        CanAttackDespiteDefenderThisTurnExecutor(),
        RedirectNextDamageExecutor(),
        RedirectCombatDamageToControllerExecutor(),
        GrantAttackBlockTaxPerCreatureTypeExecutor(),
        GrantKeywordToAttackersBlockedByExecutor(),
        SuspectExecutor(),
        RemoveSuspectedExecutor()
    )
}

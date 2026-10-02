package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing all combat-related effect executors.
 */
class CombatExecutors(
    private val amountEvaluator: DynamicAmountEvaluator,
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        RandomizedBlockerPilesExecutor(),
        MustBeBlockedExecutor(),
        ProvokeExecutor(),
        ForceBlockExecutor(),
        PreventDamageExecutor(amountEvaluator),
        PreventNextDamageLeavingAmountExecutor(amountEvaluator),
        GrantCantBeBlockedExceptByColorExecutor(predicateEvaluator = amountEvaluator.predicates),
        GrantCantBeBlockedExceptByExecutor(),
        GrantCantBeBlockedExceptByCollectionExecutor(),
        ReflectCombatDamageExecutor(),
        TauntExecutor(),
        CantAttackGroupExecutor(),
        CantBlockGroupExecutor(),
        CantAttackExecutor(),
        CantBlockExecutor(),
        RemoveFromCombatExecutor(),
        BecomeBlockingExecutor(),
        SwapBlockingAssignmentsExecutor(cardRegistry, predicateEvaluator = amountEvaluator.predicates),
        OpponentGuessesTopCardKindExecutor(),
        PlayerGuessesConditionExecutor(),
        MarkMustAttackThisTurnExecutor(),
        MarkMustBlockThisTurnExecutor(),
        GoadExecutor(),
        CanAttackDespiteDefenderThisTurnExecutor(),
        RedirectNextDamageExecutor(),
        RedirectDamageFromChosenSourceExecutor(),
        RedirectCombatDamageToControllerExecutor(),
        GrantAttackBlockTaxPerCreatureTypeExecutor(),
        GrantKeywordToAttackersBlockedByExecutor(),
        SuspectExecutor(),
        RemoveSuspectedExecutor()
    )
}

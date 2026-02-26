package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing all permanent-related effect executors.
 */
class PermanentExecutors(
    private val decisionHandler: DecisionHandler = DecisionHandler(),
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        TapUntapExecutor(),
        TapTargetCreaturesExecutor(),
        ModifyStatsExecutor(amountEvaluator),
        GrantKeywordUntilEndOfTurnExecutor(),
        RemoveKeywordUntilEndOfTurnExecutor(),
        AddCountersExecutor(),
        RemoveCountersExecutor(),
        ChooseColorProtectionExecutor(decisionHandler),
        ChooseColorProtectionTargetExecutor(decisionHandler),
        ChangeCreatureTypeTextExecutor(decisionHandler),
        GainControlExecutor(),
        GainControlByActivePlayerExecutor(),
        GainControlByMostOfSubtypeExecutor(),
        GiveControlToTargetPlayerExecutor(),
        TurnFaceDownExecutor(),
        TurnFaceUpExecutor(),
        GrantTriggeredAbilityUntilEndOfTurnExecutor(),
        GrantActivatedAbilityUntilEndOfTurnExecutor(),
        GrantToEnchantedCreatureTypeGroupExecutor(),
        BecomeCreatureTypeExecutor(),
        ChooseCreatureTypeModifyStatsExecutor(amountEvaluator),
        BecomeChosenTypeAllCreaturesExecutor(),
        SetGroupCreatureSubtypesExecutor(),
        SetCreatureSubtypesExecutor(),
        ChangeGroupColorExecutor(),
        GrantActivatedAbilityToGroupExecutor(),
        ChooseCreatureTypeUntapExecutor(),
        ChooseCreatureTypeGainControlExecutor(),
        AnimateLandExecutor()
    )
}

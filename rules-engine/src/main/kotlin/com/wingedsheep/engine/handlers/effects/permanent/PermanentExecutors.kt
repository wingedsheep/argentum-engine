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
        TapAllCreaturesExecutor(),
        TapTargetCreaturesExecutor(),
        UntapAllCreaturesYouControlExecutor(),
        ModifyStatsExecutor(),
        DynamicModifyStatsExecutor(amountEvaluator),
        ModifyStatsForGroupExecutor(),
        GrantKeywordUntilEndOfTurnExecutor(),
        GrantKeywordToGroupExecutor(),
        AddCountersExecutor(),
        RemoveCountersExecutor(),
        RemoveAllCountersOfTypeExecutor(),
        ChooseColorProtectionExecutor(decisionHandler),
        ChangeCreatureTypeTextExecutor(decisionHandler),
        GainControlExecutor(),
        GainControlByMostOfSubtypeExecutor(),
        TurnFaceDownExecutor(),
        TurnFaceUpExecutor()
    )
}

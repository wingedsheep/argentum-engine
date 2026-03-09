package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.registry.CardRegistry

/**
 * Module providing all permanent-related effect executors.
 */
class PermanentExecutors(
    private val decisionHandler: DecisionHandler = DecisionHandler(),
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val cardRegistry: CardRegistry? = null
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        TapUntapExecutor(),
        TapTargetCreaturesExecutor(),
        ModifyStatsExecutor(amountEvaluator),
        GrantKeywordExecutor(),
        RemoveKeywordExecutor(),
        AddCountersExecutor(),
        RemoveCountersExecutor(),
        ChooseColorProtectionExecutor(decisionHandler),
        ChooseColorProtectionTargetExecutor(decisionHandler),
        ChangeCreatureTypeTextExecutor(decisionHandler),
        ExchangeControlExecutor(),
        GainControlExecutor(),
        GainControlByActivePlayerExecutor(),
        GainControlByMostOfSubtypeExecutor(),
        GiveControlToTargetPlayerExecutor(),
        TurnFaceDownExecutor(),
        TurnFaceUpExecutor(cardRegistry),
        GrantTriggeredAbilityExecutor(),
        GrantActivatedAbilityExecutor(),
        GrantToEnchantedCreatureTypeGroupExecutor(),
        BecomeCreatureTypeExecutor(),
        SetGroupCreatureSubtypesExecutor(),
        SetCreatureSubtypesExecutor(),
        AddCreatureTypeExecutor(),
        ChangeGroupColorExecutor(),
        GrantActivatedAbilityToGroupExecutor(),
        AnimateLandExecutor(),
        SetBasePowerExecutor(amountEvaluator),
        DistributeCountersFromSelfExecutor(),
        DistributeCountersAmongTargetsExecutor(),
        AttachEquipmentExecutor()
    )
}

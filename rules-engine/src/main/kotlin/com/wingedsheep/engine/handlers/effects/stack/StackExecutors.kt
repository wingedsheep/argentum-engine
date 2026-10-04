package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.mechanics.stack.SpellCounterer
import com.wingedsheep.engine.registry.CardRegistry

/**
 * Module providing all stack-related effect executors.
 */
class StackExecutors(
    private val zones: ZoneTransitionService,
    private val amountEvaluator: DynamicAmountEvaluator,
    private val cardRegistry: CardRegistry,
    private val counterer: SpellCounterer,
    private val targetFinder: TargetFinder
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        CounterEffectExecutor(amountEvaluator, cardRegistry, counterer),
        ExileTargetSpellExecutor(counterer),
        ExileSpellsOnStackExecutor(counterer),
        CounterAllOnStackExecutor(counterer),
        WardCounterEffectExecutor(zones, cardRegistry, counterer),
        ChangeSpellTargetExecutor(targetFinder = targetFinder),
        ChangeTargetExecutor(targetFinder = targetFinder, predicateEvaluator = zones.predicateEvaluator),
        StormCopyEffectExecutor(targetFinder = targetFinder),
        CopyTargetSpellExecutor(dynamicAmountEvaluator = amountEvaluator, targetFinder = targetFinder),
        CopyEachTargetSpellExecutor(targetFinder = targetFinder),
        CopyForEachOtherPossibleTargetExecutor(targetFinder = targetFinder, predicateEvaluator = zones.predicateEvaluator),
        CopyTargetTriggeredAbilityExecutor(targetFinder = targetFinder),
        CopyTargetSpellOrAbilityExecutor(dynamicAmountEvaluator = amountEvaluator, targetFinder = targetFinder),
        CopyNextSpellCastExecutor(),
        CopyEachSpellCastExecutor(),
        MakeNextSpellUncounterableExecutor(),
        GrantNextSpellAffinityExecutor(),
        GrantNextSpellKeywordExecutor(),
        GrantNextSpellFreeCastExecutor(),
        ReduceSpellCostsExecutor(amountEvaluator),
        ReselectTargetRandomlyExecutor(predicateEvaluator = zones.predicateEvaluator, targetFinder = targetFinder),
        ChangeTriggeringObjectTargetsExecutor(targetFinder = targetFinder),
        GrantKeywordToSpellExecutor(),
        MarkSpellExileWithCountersExecutor(),
        MarkSpellPlotOnResolveExecutor(),
        ReturnSpellToOwnersHandExecutor(),
        ReturnSpellOrPermanentToOwnersHandExecutor(zones, cardRegistry, targetFinder = targetFinder),
        DestroySourceOfTargetedAbilityExecutor(zones),
        RemoveAbilitiesFromSourceOfTargetedAbilityExecutor()
    )
}

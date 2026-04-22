package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.handlers.effects.permanent.abilities.GrantActivatedAbilityExecutor
import com.wingedsheep.engine.handlers.effects.permanent.abilities.GrantActivatedAbilityToGroupExecutor
import com.wingedsheep.engine.handlers.effects.permanent.abilities.GrantKeywordExecutor
import com.wingedsheep.engine.handlers.effects.permanent.abilities.GrantToEnchantedCreatureTypeGroupExecutor
import com.wingedsheep.engine.handlers.effects.permanent.abilities.GrantTriggeredAbilityExecutor
import com.wingedsheep.engine.handlers.effects.permanent.abilities.IncrementAbilityResolutionCountExecutor
import com.wingedsheep.engine.handlers.effects.permanent.abilities.LevelUpClassExecutor
import com.wingedsheep.engine.handlers.effects.permanent.abilities.RemoveAllAbilitiesExecutor
import com.wingedsheep.engine.handlers.effects.permanent.abilities.RemoveKeywordExecutor
import com.wingedsheep.engine.handlers.effects.permanent.attachments.AttachEquipmentExecutor
import com.wingedsheep.engine.handlers.effects.permanent.attachments.AttachTargetEquipmentToCreatureExecutor
import com.wingedsheep.engine.handlers.effects.permanent.attachments.GrantExileOnLeaveExecutor
import com.wingedsheep.engine.handlers.effects.permanent.control.ExchangeControlExecutor
import com.wingedsheep.engine.handlers.effects.permanent.control.GainControlByActivePlayerExecutor
import com.wingedsheep.engine.handlers.effects.permanent.control.GainControlByMostOfSubtypeExecutor
import com.wingedsheep.engine.handlers.effects.permanent.control.GainControlExecutor
import com.wingedsheep.engine.handlers.effects.permanent.control.GiveControlToTargetPlayerExecutor
import com.wingedsheep.engine.handlers.effects.permanent.counters.AddCountersExecutor
import com.wingedsheep.engine.handlers.effects.permanent.counters.AddCountersToCollectionExecutor
import com.wingedsheep.engine.handlers.effects.permanent.counters.AddDynamicCountersExecutor
import com.wingedsheep.engine.handlers.effects.permanent.counters.DistributeCountersAmongTargetsExecutor
import com.wingedsheep.engine.handlers.effects.permanent.counters.DistributeCountersFromSelfExecutor
import com.wingedsheep.engine.handlers.effects.permanent.counters.RemoveCountersExecutor
import com.wingedsheep.engine.handlers.effects.permanent.protection.ChooseColorProtectionExecutor
import com.wingedsheep.engine.handlers.effects.permanent.protection.ChooseColorProtectionTargetExecutor
import com.wingedsheep.engine.handlers.effects.permanent.stats.ModifyStatsExecutor
import com.wingedsheep.engine.handlers.effects.permanent.stats.SetBasePowerExecutor
import com.wingedsheep.engine.handlers.effects.permanent.stats.SetBasePowerToughnessExecutor
import com.wingedsheep.engine.handlers.effects.permanent.tapping.TapTargetCreaturesExecutor
import com.wingedsheep.engine.handlers.effects.permanent.tapping.TapUntapCollectionExecutor
import com.wingedsheep.engine.handlers.effects.permanent.tapping.TapUntapExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.AddCardTypeExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.AddCreatureTypeExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.AddSubtypeExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.AnimateLandExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.BecomeCreatureExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.BecomeCreatureTypeExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.ChangeCreatureTypeTextExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.LoseAllCreatureTypesExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.ChangeGroupColorExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.EachPermanentBecomesCopyOfTargetExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.SetCreatureSubtypesExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.SetGroupCreatureSubtypesExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.TransformEffectExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.TurnFaceDownExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.TurnFaceUpExecutor
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry

/**
 * Module providing all permanent-related effect executors.
 *
 * Executors are organized into topical subpackages by concern:
 *  - `counters/` — counter placement and removal
 *  - `control/` — control-changing effects
 *  - `types/` — card type, creature type, color changes
 *  - `attachments/` — equipment / aura attachment bookkeeping
 *  - `stats/` — power/toughness modification
 *  - `abilities/` — granting/removing abilities and keywords
 *  - `tapping/` — tap/untap state changes
 *  - `protection/` — color protection
 */
class PermanentExecutors(
    private val decisionHandler: DecisionHandler = DecisionHandler(),
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val cardRegistry: CardRegistry
) : ExecutorModule {
    private val staticAbilityHandler = StaticAbilityHandler(cardRegistry)

    override fun executors(): List<EffectExecutor<*>> = listOf(
        // counters
        AddCountersExecutor(),
        AddDynamicCountersExecutor(),
        AddCountersToCollectionExecutor(),
        RemoveCountersExecutor(),
        DistributeCountersFromSelfExecutor(),
        DistributeCountersAmongTargetsExecutor(),
        // control
        ExchangeControlExecutor(),
        GainControlExecutor(),
        GainControlByActivePlayerExecutor(),
        GainControlByMostOfSubtypeExecutor(),
        GiveControlToTargetPlayerExecutor(),
        // types
        AddCardTypeExecutor(),
        AddCreatureTypeExecutor(),
        AddSubtypeExecutor(),
        AnimateLandExecutor(),
        BecomeCreatureExecutor(),
        BecomeCreatureTypeExecutor(),
        ChangeCreatureTypeTextExecutor(decisionHandler),
        ChangeGroupColorExecutor(),
        EachPermanentBecomesCopyOfTargetExecutor(),
        LoseAllCreatureTypesExecutor(),
        SetCreatureSubtypesExecutor(),
        SetGroupCreatureSubtypesExecutor(),
        TransformEffectExecutor(cardRegistry),
        TurnFaceDownExecutor(),
        TurnFaceUpExecutor(cardRegistry),
        // attachments
        AttachEquipmentExecutor(),
        AttachTargetEquipmentToCreatureExecutor(),
        GrantExileOnLeaveExecutor(),
        // stats
        ModifyStatsExecutor(amountEvaluator),
        SetBasePowerExecutor(amountEvaluator),
        SetBasePowerToughnessExecutor(),
        // abilities
        GrantActivatedAbilityExecutor(),
        GrantActivatedAbilityToGroupExecutor(),
        GrantKeywordExecutor(),
        RemoveKeywordExecutor(),
        GrantToEnchantedCreatureTypeGroupExecutor(),
        GrantTriggeredAbilityExecutor(),
        RemoveAllAbilitiesExecutor(),
        LevelUpClassExecutor(staticAbilityHandler),
        IncrementAbilityResolutionCountExecutor(),
        // tapping
        TapUntapExecutor(),
        TapTargetCreaturesExecutor(),
        TapUntapCollectionExecutor(),
        // protection
        ChooseColorProtectionExecutor(decisionHandler),
        ChooseColorProtectionTargetExecutor(decisionHandler)
    )
}

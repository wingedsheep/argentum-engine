package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.handlers.effects.permanent.abilities.GrantActivatedAbilityExecutor
import com.wingedsheep.engine.handlers.effects.permanent.abilities.GrantActivatedAbilityToGroupExecutor
import com.wingedsheep.engine.handlers.effects.permanent.abilities.GrantEmbalmExecutor
import com.wingedsheep.engine.handlers.effects.permanent.abilities.GrantFlashbackExecutor
import com.wingedsheep.engine.handlers.effects.permanent.abilities.GrantHarmonizeExecutor
import com.wingedsheep.engine.handlers.effects.permanent.abilities.GrantKeywordExecutor
import com.wingedsheep.engine.handlers.effects.permanent.abilities.GrantReplacementEffectExecutor
import com.wingedsheep.engine.handlers.effects.permanent.abilities.GrantStaticAbilityExecutor
import com.wingedsheep.engine.handlers.effects.permanent.abilities.GrantToEnchantedCreatureTypeGroupExecutor
import com.wingedsheep.engine.handlers.effects.permanent.abilities.GrantTriggeredAbilityExecutor
import com.wingedsheep.engine.handlers.effects.permanent.abilities.IncrementAbilityResolutionCountExecutor
import com.wingedsheep.engine.handlers.effects.permanent.abilities.MarkEnduringReturnExecutor
import com.wingedsheep.engine.handlers.effects.permanent.abilities.LevelUpClassExecutor
import com.wingedsheep.engine.handlers.effects.permanent.abilities.RemoveAllAbilitiesExecutor
import com.wingedsheep.engine.handlers.effects.permanent.abilities.RemoveKeywordExecutor
import com.wingedsheep.engine.handlers.effects.permanent.attachments.AttachEquipmentExecutor
import com.wingedsheep.engine.handlers.effects.permanent.attachments.AttachTargetEquipmentToCreatureExecutor
import com.wingedsheep.engine.handlers.effects.permanent.attachments.UnattachEquipmentExecutor
import com.wingedsheep.engine.handlers.effects.permanent.attachments.GrantExileOnLeaveExecutor
import com.wingedsheep.engine.handlers.effects.permanent.control.ExchangeControlExecutor
import com.wingedsheep.engine.handlers.effects.permanent.control.GainControlByActivePlayerExecutor
import com.wingedsheep.engine.handlers.effects.permanent.control.GainControlByRankExecutor
import com.wingedsheep.engine.handlers.effects.permanent.control.GainControlExecutor
import com.wingedsheep.engine.handlers.effects.permanent.control.GiveControlToTargetPlayerExecutor
import com.wingedsheep.engine.handlers.effects.permanent.counters.AddCountersExecutor
import com.wingedsheep.engine.handlers.effects.permanent.counters.AddCountersToCollectionExecutor
import com.wingedsheep.engine.handlers.effects.permanent.counters.AddDynamicCountersExecutor
import com.wingedsheep.engine.handlers.effects.permanent.counters.MoveAllLastKnownCountersExecutor
import com.wingedsheep.engine.handlers.effects.permanent.counters.MoveCountersEachKindMissingExecutor
import com.wingedsheep.engine.handlers.effects.permanent.counters.MoveCountersExecutor
import com.wingedsheep.engine.handlers.effects.permanent.counters.MoveChosenCountersToTargetExecutor
import com.wingedsheep.engine.handlers.effects.permanent.counters.DistributeCountersAmongFilteredExecutor
import com.wingedsheep.engine.handlers.effects.permanent.counters.DistributeCountersAmongTargetsExecutor
import com.wingedsheep.engine.handlers.effects.permanent.counters.DoubleCountersExecutor
import com.wingedsheep.engine.handlers.effects.permanent.counters.GrantCounterPlacementModifierExecutor
import com.wingedsheep.engine.handlers.effects.permanent.counters.DistributeCountersFromSelfExecutor
import com.wingedsheep.engine.handlers.effects.permanent.counters.ProliferateExecutor
import com.wingedsheep.engine.handlers.effects.permanent.counters.RemoveAllCountersExecutor
import com.wingedsheep.engine.handlers.effects.permanent.counters.RemoveAllCountersOfTypeExecutor
import com.wingedsheep.engine.handlers.effects.permanent.counters.RemoveAnyNumberOfCountersExecutor
import com.wingedsheep.engine.handlers.effects.permanent.counters.RemoveCountersExecutor
import com.wingedsheep.engine.handlers.effects.permanent.protection.ChooseColorThenExecutor
import com.wingedsheep.engine.handlers.effects.permanent.protection.GrantCantBeBlockedByChosenColorExecutor
import com.wingedsheep.engine.handlers.effects.permanent.protection.GrantHexproofFromChosenColorExecutor
import com.wingedsheep.engine.handlers.effects.permanent.protection.GrantProtectionFromChosenColorExecutor
import com.wingedsheep.engine.handlers.effects.permanent.protection.GrantProtectionFromChosenCardTypeExecutor
import com.wingedsheep.engine.handlers.effects.permanent.stats.ModifyStatsExecutor
import com.wingedsheep.engine.handlers.effects.permanent.stats.SetBaseStatsExecutor
import com.wingedsheep.engine.handlers.effects.permanent.phasing.PhaseOutExecutor
import com.wingedsheep.engine.handlers.effects.permanent.phasing.PhaseOutUntilLeavesExecutor
import com.wingedsheep.engine.handlers.effects.permanent.phasing.PhaseInLinkedToSourceExecutor
import com.wingedsheep.engine.handlers.effects.permanent.tapping.TapUntapCollectionExecutor
import com.wingedsheep.engine.handlers.effects.permanent.room.LockDoorExecutor
import com.wingedsheep.engine.handlers.effects.permanent.room.UnlockDoorExecutor
import com.wingedsheep.engine.handlers.effects.permanent.tapping.TapUntapExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.AddCardTypeExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.AddColorExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.AddCreatureTypeExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.AddSubtypeExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.AnimateLandExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.BecomeChosenManaColorExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.BecomeArtifactExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.BecomeCreatureExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.BecomeCreatureTypeExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.BecomePreparedExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.UnprepareExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.BecomeSaddledExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.BecomeSolvedExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.ChangeCreatureTypeTextExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.ChangeWordInTextExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.ChooseColorForTargetExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.LoseAllCreatureTypesExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.MassAnimateExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.ChangeColorExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.ChangeColorToChosenExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.ChangeGroupColorExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.BecomeCopyOfLinkedExileExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.EachPermanentBecomesCopyOfTargetExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.SetCreatureSubtypesExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.SetGroupCreatureSubtypesExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.SetLandTypeExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.ExileAndReturnTransformedExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.ReturnSelfFromExileTransformedExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.ReturnSelfFromZoneTransformedExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.TransformEffectExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.TurnFaceDownExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.TurnFaceUpExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.RevealFaceDownPermanentExecutor
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

    // Late-bound registry recursion, so ExploreEffectExecutor / ConniveEffectExecutor can re-issue
    // their action as a Composite(prefixEffect, action) when a ModifyKeywordAction replacement
    // (CR 614) applies, and so the connive pipeline itself can be run. Mirrors
    // LibraryExecutors' recursion wiring; read through the ref at execution time so constructing
    // this module before initialization (as some unit tests do) never trips over an unset property.
    private val recursionRef =
        java.util.concurrent.atomic.AtomicReference<((com.wingedsheep.engine.state.GameState, com.wingedsheep.sdk.scripting.effects.Effect, com.wingedsheep.engine.handlers.EffectContext) -> com.wingedsheep.engine.core.EffectResult)?>(null)

    private val recursion: (com.wingedsheep.engine.state.GameState, com.wingedsheep.sdk.scripting.effects.Effect, com.wingedsheep.engine.handlers.EffectContext) -> com.wingedsheep.engine.core.EffectResult =
        { state, effect, context ->
            val executor = recursionRef.get()
                ?: error("PermanentExecutors.initializeRecursion(...) was not called before a keyword-action executor ran")
            executor(state, effect, context)
        }

    /** Late-bind the registry's recursive executor so the keyword-action executors can delegate. */
    fun initializeRecursion(executor: (com.wingedsheep.engine.state.GameState, com.wingedsheep.sdk.scripting.effects.Effect, com.wingedsheep.engine.handlers.EffectContext) -> com.wingedsheep.engine.core.EffectResult) {
        recursionRef.set(executor)
    }

    override fun executors(): List<EffectExecutor<*>> = listOf(
        // counters
        AddCountersExecutor(),
        AddDynamicCountersExecutor(),
        com.wingedsheep.engine.handlers.effects.permanent.counters.AddCountersUpToExecutor(),
        MoveAllLastKnownCountersExecutor(),
        AddCountersToCollectionExecutor(),
        DoubleCountersExecutor(),
        GrantCounterPlacementModifierExecutor(),
        RemoveCountersExecutor(),
        RemoveAnyNumberOfCountersExecutor(),
        com.wingedsheep.engine.handlers.effects.permanent.counters.PayCountersExecutor(),
        com.wingedsheep.engine.handlers.effects.permanent.counters.PayFixedCountersExecutor(),
        com.wingedsheep.engine.handlers.effects.permanent.counters.ConvertCountersToTokensExecutor(),
        MoveCountersEachKindMissingExecutor(),
        MoveCountersExecutor(),
        MoveChosenCountersToTargetExecutor(),
        RemoveAllCountersExecutor(),
        RemoveAllCountersOfTypeExecutor(),
        DistributeCountersFromSelfExecutor(),
        DistributeCountersAmongTargetsExecutor(),
        DistributeCountersAmongFilteredExecutor(),
        ProliferateExecutor(),
        com.wingedsheep.engine.handlers.effects.permanent.counters.EmitTrainedEventExecutor(),
        // control
        ExchangeControlExecutor(),
        GainControlExecutor(),
        GainControlByActivePlayerExecutor(),
        GainControlByRankExecutor(recursion),
        GiveControlToTargetPlayerExecutor(),
        // types
        AddCardTypeExecutor(),
        AddColorExecutor(),
        AddCreatureTypeExecutor(),
        AddSubtypeExecutor(),
        SetLandTypeExecutor(),
        AnimateLandExecutor(),
        BecomeArtifactExecutor(staticAbilityHandler),
        BecomeChosenManaColorExecutor(),
        BecomeCreatureExecutor(),
        BecomeSaddledExecutor(),
        BecomeSolvedExecutor(),
        BecomePreparedExecutor(cardRegistry),
        UnprepareExecutor(),
        BecomeCreatureTypeExecutor(),
        ChangeCreatureTypeTextExecutor(decisionHandler),
        ChangeWordInTextExecutor(decisionHandler),
        ChooseColorForTargetExecutor(decisionHandler),
        ChangeColorExecutor(),
        ChangeColorToChosenExecutor(),
        ChangeGroupColorExecutor(),
        EachPermanentBecomesCopyOfTargetExecutor(cardRegistry),
        BecomeCopyOfLinkedExileExecutor(),
        LoseAllCreatureTypesExecutor(),
        MassAnimateExecutor(),
        SetCreatureSubtypesExecutor(),
        SetGroupCreatureSubtypesExecutor(),
        TransformEffectExecutor(cardRegistry),
        ReturnSelfFromExileTransformedExecutor(cardRegistry),
        ReturnSelfFromZoneTransformedExecutor(cardRegistry),
        ExileAndReturnTransformedExecutor(cardRegistry),
        TurnFaceDownExecutor(),
        TurnFaceUpExecutor(cardRegistry),
        RevealFaceDownPermanentExecutor(),
        // attachments
        AttachEquipmentExecutor(),
        AttachTargetEquipmentToCreatureExecutor(),
        UnattachEquipmentExecutor(),
        GrantExileOnLeaveExecutor(),
        // stats
        ModifyStatsExecutor(amountEvaluator),
        SetBaseStatsExecutor(amountEvaluator),
        // abilities
        GrantActivatedAbilityExecutor(),
        GrantActivatedAbilityToGroupExecutor(),
        GrantEmbalmExecutor(),
        GrantFlashbackExecutor(),
        GrantHarmonizeExecutor(),
        GrantKeywordExecutor(),
        GrantStaticAbilityExecutor(),
        GrantReplacementEffectExecutor(),
        RemoveKeywordExecutor(),
        GrantToEnchantedCreatureTypeGroupExecutor(),
        GrantTriggeredAbilityExecutor(),
        RemoveAllAbilitiesExecutor(),
        LevelUpClassExecutor(staticAbilityHandler),
        IncrementAbilityResolutionCountExecutor(),
        MarkEnduringReturnExecutor(),
        ExploreEffectExecutor(recursion),
        EmitExploredEventExecutor(),
        // connive (CR 701.50) — same shape as explore: the executor consults
        // ModifyKeywordAction replacements and delegates the pipeline through [recursion]
        ConniveEffectExecutor(recursion),
        EmitConnivedEventExecutor(),
        // tapping
        TapUntapExecutor(),
        TapUntapCollectionExecutor(),
        // rooms / doors
        UnlockDoorExecutor(staticAbilityHandler),
        LockDoorExecutor(staticAbilityHandler),
        // soulbond
        PairWithSourceExecutor(),
        // phasing
        PhaseOutExecutor(),
        PhaseOutUntilLeavesExecutor(),
        PhaseInLinkedToSourceExecutor(),
        // protection
        ChooseColorThenExecutor(decisionHandler),
        GrantHexproofFromChosenColorExecutor(),
        GrantProtectionFromChosenColorExecutor(),
        GrantProtectionFromChosenCardTypeExecutor(),
        GrantCantBeBlockedByChosenColorExecutor()
    )
}

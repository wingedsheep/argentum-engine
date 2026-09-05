package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.ObjectReferenceEnvironment
import com.wingedsheep.engine.state.GameState

internal fun ContinuationFrame.objectReferences(): ObjectReferenceEnvironment? = when (this) {
    is CounterUnlessPaysContinuation -> objectReferences
    is MayPayManaContinuation -> effectContext.objectReferences
    is MayPayManaSelectionContinuation -> effectContext.objectReferences
    is MayPayXContinuation -> effectContext.objectReferences
    is CounterUnlessPaysManaSelectionContinuation -> objectReferences
    is WardTapPermanentsSubCostContinuation -> objectReferences
    is AddDynamicManaContinuation -> objectReferences
    is AddManaPipsContinuation -> objectReferences
    is ChooseNumberForSourceContinuation -> objectReferences
    is ChooseOpponentForSourceContinuation -> objectReferences
    is ChooseCardTypeForSourceContinuation -> objectReferences
    is ChooseOpponentDeciderContinuation -> baseContext.objectReferences
    is PutFromHandContinuation -> objectReferences
    is SecretBidContinuation -> objectReferences
    is OpenLifeBidContinuation -> objectReferences
    is ContestedRetargetContinuation -> objectReferences
    is DistributeCountersContinuation -> objectReferences
    is RemoveAnyNumberOfCountersContinuation -> objectReferences
    is AddCountersUpToContinuation -> objectReferences
    is PayCountersContinuation -> objectReferences
    is MoveChosenCountersToTargetContinuation -> objectReferences
    is AmassContinuation -> objectReferences
    is StormCopyTargetContinuation -> objectReferences
    is StormCopyModalTargetContinuation -> objectReferences
    is ChangeSpellTargetContinuation -> objectReferences
    is DistributeDamageContinuation -> objectReferences
    is DeflectDamageSourceChoiceContinuation -> objectReferences
    is PreventDamageFromChosenSourceContinuation -> objectReferences
    is OptionalRedirectEffectContinuation -> effectContext.objectReferences
    is EachPlayerDiscardsOrLoseLifeContinuation -> objectReferences
    is DrawUpToContinuation -> objectReferences
    is StaticDrawReplacementContinuation -> objectReferences
    is ChainCopyAfterActionContinuation -> objectReferences
    is ChainCopyDecisionContinuation -> objectReferences
    is ChainCopyCostContinuation -> objectReferences
    is ChainCopyTargetContinuation -> objectReferences
    is SacrificeContinuation -> objectReferences
    is ChooseOnePerCategoryContinuation -> objectReferences
    is ExileMultiZoneContinuation -> objectReferences
    is PayOrSufferContinuation -> objectReferences
    is PayOrSufferChoiceContinuation -> objectReferences
    is AnyPlayerMayPayContinuation -> objectReferences
    is ReturnFromGraveyardContinuation -> objectReferences
    is CostPaymentContinuation -> objectReferences
    is ChooseColorThenContinuation -> baseContext.objectReferences
    is ChooseNumberThenContinuation -> baseContext.objectReferences
    is ChooseManaColorContinuation -> baseContext.objectReferences
    is ChooseColorForTargetContinuation -> objectReferences
    is ChooseReplacementContinuation -> objectReferences
    is BecomeCreatureTypeContinuation -> objectReferences
    is ChooseCardTypeForProtectionContinuation -> objectReferences
    is EachPlayerChoosesCreatureTypeContinuation -> objectReferences
    is SelectFromCollectionContinuation -> objectReferences
    is MoveCollectionOrderContinuation -> objectReferences
    is SelectTargetPipelineContinuation -> objectReferences
    is ChooseOptionPipelineContinuation -> objectReferences
    is NoteCreatureTypePipelineContinuation -> objectReferences
    is ChoosePileContinuation -> objectReferences
    is MoveCollectionAuraTargetContinuation -> objectReferences
    is PutOnBottomOfLibraryContinuation -> objectReferences
    is PutOnTopOrBottomContinuation -> objectReferences
    is ReturnFromLinkedExileContinuation -> objectReferences
    is CascadeMayCastContinuation -> objectReferences
    is DiscoverMayCastContinuation -> objectReferences
    is CastAnyNumberFromCollectionContinuation -> effectContext.objectReferences
    is ReplacementChoiceContinuation -> context?.objectReferences
    is TokenCreationReplacementContinuation -> effectContext.objectReferences
    is ChooseGuessKindContinuation -> effectContext.objectReferences
    is GuessTopCardKindContinuation -> effectContext.objectReferences
    is GuessConditionContinuation -> effectContext.objectReferences
    is ModalContinuation -> objectReferences
    is ModalPreChosenContinuation -> objectReferences
    is SpliceTailContinuation -> objectReferences
    is ModalChosenModeTailContinuation -> objectReferences
    is ModalTargetContinuation -> objectReferences
    is BudgetModalContinuation -> objectReferences
    is CreateTokenCopyOfChosenContinuation -> objectReferences
    is CreateTokenCopyAuraHostContinuation -> context.objectReferences
    is CreateTokenCopyRemainingContinuation -> context.objectReferences
    is ChooseActionContinuation -> objectReferences
    is EffectContinuation -> effectContext.objectReferences
    is TriggeredAbilityContinuation -> objectReferences
    is TriggerDamageDistributionContinuation -> objectReferences
    is GatedActionContinuation -> effectContext.objectReferences
    is MayAbilityContinuation -> effectContext.objectReferences
    is GatedEffectContinuation -> effectContext.objectReferences
    is MayRevealCardFromHandContinuation -> effectContext.objectReferences
    is BeholdContinuation -> effectContext.objectReferences
    is ForEachContinuation -> effectContext.objectReferences
    is RepeatWhileContinuation -> effectContext.objectReferences
    is FlipCoinsUntilLossContinuation -> objectReferences
    is CoinFlipChoiceContinuation -> effectContext.objectReferences
    is ReflexiveTriggerTargetContinuation -> effectContext.objectReferences
    is ConvertCountersToTokensContinuation -> objectReferences
    else -> null
}

internal fun ContinuationFrame.withObjectReferences(refs: ObjectReferenceEnvironment): ContinuationFrame = when (this) {
    is CounterUnlessPaysContinuation -> copy(objectReferences = refs)
    is MayPayManaContinuation -> copy(effectContext = effectContext.copy(objectReferences = refs))
    is MayPayManaSelectionContinuation -> copy(effectContext = effectContext.copy(objectReferences = refs))
    is MayPayXContinuation -> copy(effectContext = effectContext.copy(objectReferences = refs))
    is CounterUnlessPaysManaSelectionContinuation -> copy(objectReferences = refs)
    is WardTapPermanentsSubCostContinuation -> copy(objectReferences = refs)
    is AddDynamicManaContinuation -> copy(objectReferences = refs)
    is AddManaPipsContinuation -> copy(objectReferences = refs)
    is ChooseNumberForSourceContinuation -> copy(objectReferences = refs)
    is ChooseOpponentForSourceContinuation -> copy(objectReferences = refs)
    is ChooseCardTypeForSourceContinuation -> copy(objectReferences = refs)
    is ChooseOpponentDeciderContinuation -> copy(baseContext = baseContext.copy(objectReferences = refs))
    is PutFromHandContinuation -> copy(objectReferences = refs)
    is SecretBidContinuation -> copy(objectReferences = refs)
    is OpenLifeBidContinuation -> copy(objectReferences = refs)
    is ContestedRetargetContinuation -> copy(objectReferences = refs)
    is DistributeCountersContinuation -> copy(objectReferences = refs)
    is RemoveAnyNumberOfCountersContinuation -> copy(objectReferences = refs)
    is AddCountersUpToContinuation -> copy(objectReferences = refs)
    is PayCountersContinuation -> copy(objectReferences = refs)
    is MoveChosenCountersToTargetContinuation -> copy(objectReferences = refs)
    is AmassContinuation -> copy(objectReferences = refs)
    is StormCopyTargetContinuation -> copy(objectReferences = refs)
    is StormCopyModalTargetContinuation -> copy(objectReferences = refs)
    is ChangeSpellTargetContinuation -> copy(objectReferences = refs)
    is DistributeDamageContinuation -> copy(objectReferences = refs)
    is DeflectDamageSourceChoiceContinuation -> copy(objectReferences = refs)
    is PreventDamageFromChosenSourceContinuation -> copy(objectReferences = refs)
    is OptionalRedirectEffectContinuation -> copy(effectContext = effectContext.copy(objectReferences = refs))
    is EachPlayerDiscardsOrLoseLifeContinuation -> copy(objectReferences = refs)
    is DrawUpToContinuation -> copy(objectReferences = refs)
    is StaticDrawReplacementContinuation -> copy(objectReferences = refs)
    is ChainCopyAfterActionContinuation -> copy(objectReferences = refs)
    is ChainCopyDecisionContinuation -> copy(objectReferences = refs)
    is ChainCopyCostContinuation -> copy(objectReferences = refs)
    is ChainCopyTargetContinuation -> copy(objectReferences = refs)
    is SacrificeContinuation -> copy(objectReferences = refs)
    is ChooseOnePerCategoryContinuation -> copy(objectReferences = refs)
    is ExileMultiZoneContinuation -> copy(objectReferences = refs)
    is PayOrSufferContinuation -> copy(objectReferences = refs)
    is PayOrSufferChoiceContinuation -> copy(objectReferences = refs)
    is AnyPlayerMayPayContinuation -> copy(objectReferences = refs)
    is ReturnFromGraveyardContinuation -> copy(objectReferences = refs)
    is CostPaymentContinuation -> copy(objectReferences = refs)
    is ChooseColorThenContinuation -> copy(baseContext = baseContext.copy(objectReferences = refs))
    is ChooseNumberThenContinuation -> copy(baseContext = baseContext.copy(objectReferences = refs))
    is ChooseManaColorContinuation -> copy(baseContext = baseContext.copy(objectReferences = refs))
    is ChooseColorForTargetContinuation -> copy(objectReferences = refs)
    is ChooseReplacementContinuation -> copy(objectReferences = refs)
    is BecomeCreatureTypeContinuation -> copy(objectReferences = refs)
    is ChooseCardTypeForProtectionContinuation -> copy(objectReferences = refs)
    is EachPlayerChoosesCreatureTypeContinuation -> copy(objectReferences = refs)
    is SelectFromCollectionContinuation -> copy(objectReferences = refs)
    is MoveCollectionOrderContinuation -> copy(objectReferences = refs)
    is SelectTargetPipelineContinuation -> copy(objectReferences = refs)
    is ChooseOptionPipelineContinuation -> copy(objectReferences = refs)
    is NoteCreatureTypePipelineContinuation -> copy(objectReferences = refs)
    is ChoosePileContinuation -> copy(objectReferences = refs)
    is MoveCollectionAuraTargetContinuation -> copy(objectReferences = refs)
    is PutOnBottomOfLibraryContinuation -> copy(objectReferences = refs)
    is PutOnTopOrBottomContinuation -> copy(objectReferences = refs)
    is ReturnFromLinkedExileContinuation -> copy(objectReferences = refs)
    is CascadeMayCastContinuation -> copy(objectReferences = refs)
    is DiscoverMayCastContinuation -> copy(objectReferences = refs)
    is CastAnyNumberFromCollectionContinuation -> copy(effectContext = effectContext.copy(objectReferences = refs))
    is ReplacementChoiceContinuation -> copy(context = context?.copy(objectReferences = refs))
    is TokenCreationReplacementContinuation -> copy(effectContext = effectContext.copy(objectReferences = refs))
    is ChooseGuessKindContinuation -> copy(effectContext = effectContext.copy(objectReferences = refs))
    is GuessTopCardKindContinuation -> copy(effectContext = effectContext.copy(objectReferences = refs))
    is GuessConditionContinuation -> copy(effectContext = effectContext.copy(objectReferences = refs))
    is ModalContinuation -> copy(objectReferences = refs)
    is ModalPreChosenContinuation -> copy(objectReferences = refs)
    is SpliceTailContinuation -> copy(objectReferences = refs)
    is ModalChosenModeTailContinuation -> copy(objectReferences = refs)
    is ModalTargetContinuation -> copy(objectReferences = refs)
    is BudgetModalContinuation -> copy(objectReferences = refs)
    is CreateTokenCopyOfChosenContinuation -> copy(objectReferences = refs)
    is CreateTokenCopyAuraHostContinuation -> copy(context = context.copy(objectReferences = refs))
    is CreateTokenCopyRemainingContinuation -> copy(context = context.copy(objectReferences = refs))
    is ChooseActionContinuation -> copy(objectReferences = refs)
    is EffectContinuation -> copy(effectContext = effectContext.copy(objectReferences = refs))
    is TriggeredAbilityContinuation -> copy(objectReferences = refs)
    is TriggerDamageDistributionContinuation -> copy(objectReferences = refs)
    is GatedActionContinuation -> copy(effectContext = effectContext.copy(objectReferences = refs))
    is MayAbilityContinuation -> copy(effectContext = effectContext.copy(objectReferences = refs))
    is GatedEffectContinuation -> copy(effectContext = effectContext.copy(objectReferences = refs))
    is MayRevealCardFromHandContinuation -> copy(effectContext = effectContext.copy(objectReferences = refs))
    is BeholdContinuation -> copy(effectContext = effectContext.copy(objectReferences = refs))
    is ForEachContinuation -> copy(effectContext = effectContext.copy(objectReferences = refs))
    is RepeatWhileContinuation -> copy(effectContext = effectContext.copy(objectReferences = refs))
    is FlipCoinsUntilLossContinuation -> copy(objectReferences = refs)
    is CoinFlipChoiceContinuation -> copy(effectContext = effectContext.copy(objectReferences = refs))
    is ReflexiveTriggerTargetContinuation -> copy(effectContext = effectContext.copy(objectReferences = refs))
    is ConvertCountersToTokensContinuation -> copy(objectReferences = refs)
    else -> this
}

/** Update only suspended frames belonging to this resolution, never a nested unrelated ability. */
internal fun propagateObjectReferences(state: GameState, refs: ObjectReferenceEnvironment): GameState {
    val key = refs.resolutionKey ?: return state
    if (refs.permittedMoves.isEmpty() || state.continuationStack.isEmpty()) return state
    var changed = false
    val frames = state.continuationStack.map { frame ->
        val current = frame.objectReferences()
        if (current?.resolutionKey != key || refs.permittedMoves.all { it in current.permittedMoves }) frame
        else {
            changed = true
            frame.withObjectReferences(current.copy(permittedMoves = (current.permittedMoves + refs.permittedMoves).distinct()))
        }
    }
    return if (changed) state.copy(continuationStack = frames) else state
}

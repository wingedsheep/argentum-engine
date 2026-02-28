package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.combat.*
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.player.*
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.engine.mechanics.layers.ContinuousEffectSourceComponent
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Serializers module for all engine types that require polymorphic serialization.
 *
 * This module must be registered with Json instances that need to serialize/deserialize
 * GameAction, GameEvent, and other polymorphic types from the engine.
 *
 * Usage:
 * ```kotlin
 * val json = Json {
 *     serializersModule = engineSerializersModule
 * }
 * ```
 */
val engineSerializersModule = SerializersModule {
    // GameAction hierarchy
    polymorphic(GameAction::class) {
        subclass(PassPriority::class)
        subclass(CastSpell::class)
        subclass(ActivateAbility::class)
        subclass(PlayLand::class)
        subclass(DeclareAttackers::class)
        subclass(DeclareBlockers::class)
        subclass(OrderBlockers::class)
        subclass(MakeChoice::class)
        subclass(SelectTargets::class)
        subclass(ChooseManaColor::class)
        subclass(SubmitDecision::class)
        subclass(TakeMulligan::class)
        subclass(KeepHand::class)
        subclass(BottomCards::class)
        subclass(Concede::class)
    }

    // GameEvent hierarchy
    polymorphic(GameEvent::class) {
        subclass(ZoneChangeEvent::class)
        subclass(LifeChangedEvent::class)
        subclass(DamageDealtEvent::class)
        subclass(SpellCastEvent::class)
        subclass(AbilityActivatedEvent::class)
        subclass(AbilityTriggeredEvent::class)
        subclass(ResolvedEvent::class)
        subclass(SpellCounteredEvent::class)
        subclass(SpellFizzledEvent::class)
        subclass(AbilityResolvedEvent::class)
        subclass(AbilityFizzledEvent::class)
        subclass(AttackersDeclaredEvent::class)
        subclass(BlockersDeclaredEvent::class)
        subclass(BlockerOrderDeclaredEvent::class)
        subclass(DamageAssignedEvent::class)
        subclass(PhaseChangedEvent::class)
        subclass(StepChangedEvent::class)
        subclass(TurnChangedEvent::class)
        subclass(PriorityChangedEvent::class)
        subclass(TappedEvent::class)
        subclass(UntappedEvent::class)
        subclass(CountersAddedEvent::class)
        subclass(CountersRemovedEvent::class)
        subclass(CardsDrawnEvent::class)
        subclass(CardRevealedFromDrawEvent::class)
        subclass(DrawFailedEvent::class)
        subclass(CardsDiscardedEvent::class)
        subclass(DiscardRequiredEvent::class)
        subclass(LibraryShuffledEvent::class)
        subclass(DecisionRequestedEvent::class)
        subclass(DecisionSubmittedEvent::class)
        subclass(GameEndedEvent::class)
        subclass(PlayerLostEvent::class)
        subclass(CreatureDestroyedEvent::class)
        subclass(ManaAddedEvent::class)
        subclass(ManaSpentEvent::class)
    }

    // PendingDecision hierarchy
    polymorphic(PendingDecision::class) {
        subclass(ChooseTargetsDecision::class)
        subclass(SelectCardsDecision::class)
        subclass(YesNoDecision::class)
        subclass(ChooseModeDecision::class)
        subclass(ChooseColorDecision::class)
        subclass(ChooseNumberDecision::class)
        subclass(DistributeDecision::class)
        subclass(OrderObjectsDecision::class)
        subclass(SplitPilesDecision::class)
        subclass(ChooseOptionDecision::class)
        subclass(AssignDamageDecision::class)
        subclass(SearchLibraryDecision::class)
    }

    // DecisionResponse hierarchy
    polymorphic(DecisionResponse::class) {
        subclass(TargetsResponse::class)
        subclass(CardsSelectedResponse::class)
        subclass(YesNoResponse::class)
        subclass(ModesChosenResponse::class)
        subclass(ColorChosenResponse::class)
        subclass(NumberChosenResponse::class)
        subclass(DistributionResponse::class)
        subclass(OrderedResponse::class)
        subclass(PilesSplitResponse::class)
        subclass(OptionChosenResponse::class)
        subclass(DamageAssignmentResponse::class)
    }

    // ContinuationFrame hierarchy
    polymorphic(ContinuationFrame::class) {
        subclass(DiscardContinuation::class)
        subclass(EffectContinuation::class)
        subclass(TriggeredAbilityContinuation::class)
        subclass(DamageAssignmentContinuation::class)
        subclass(ResolveSpellContinuation::class)
        subclass(SacrificeContinuation::class)
        subclass(MayAbilityContinuation::class)
        subclass(HandSizeDiscardContinuation::class)
        subclass(BlockerOrderContinuation::class)
        subclass(PayOrSufferContinuation::class)
        subclass(ChooseColorProtectionContinuation::class)
        subclass(ChooseColorProtectionTargetContinuation::class)
        subclass(ChooseFromCreatureTypeContinuation::class)
        subclass(ChooseToCreatureTypeContinuation::class)
        subclass(PutFromHandContinuation::class)
        subclass(UntapChoiceContinuation::class)
        subclass(PendingTriggersContinuation::class)
        subclass(BecomeCreatureTypeContinuation::class)
        subclass(ChooseCreatureTypeModifyStatsContinuation::class)
        subclass(ChooseCreatureTypeGainControlContinuation::class)
        subclass(BecomeChosenTypeAllCreaturesContinuation::class)
        subclass(CounterUnlessPaysContinuation::class)
        subclass(ModalContinuation::class)
        subclass(ModalTargetContinuation::class)
        subclass(CloneEntersContinuation::class)
        subclass(ChooseColorEntersContinuation::class)
        subclass(ChooseCreatureTypeEntersContinuation::class)
        subclass(AmplifyEntersContinuation::class)
        subclass(CastWithCreatureTypeContinuation::class)
        subclass(MoveCollectionAuraTargetContinuation::class)
        subclass(ChainCopyPrimaryDiscardContinuation::class)
        subclass(ChainCopyDecisionContinuation::class)
        subclass(ChainCopyCostContinuation::class)
        subclass(ChainCopyTargetContinuation::class)
        subclass(DamagePreventionContinuation::class)
        subclass(EachPlayerChoosesCreatureTypeContinuation::class)
        subclass(PatriarchsBiddingContinuation::class)
        subclass(ChooseCreatureTypeUntapContinuation::class)
        subclass(ChangeSpellTargetContinuation::class)
        subclass(SecretBidContinuation::class)
        subclass(DrawReplacementRemainingDrawsContinuation::class)
        subclass(DrawReplacementActivationContinuation::class)
        subclass(DrawReplacementTargetContinuation::class)
        subclass(ReadTheRunesContinuation::class)
        subclass(ForEachTargetContinuation::class)
        subclass(ForEachPlayerContinuation::class)
        subclass(DrawUpToContinuation::class)
        subclass(RepeatWhileContinuation::class)
        subclass(SelectFromCollectionContinuation::class)
        subclass(MoveCollectionOrderContinuation::class)
        subclass(ChooseCreatureTypePipelineContinuation::class)
        subclass(ChooseOptionPipelineContinuation::class)
        subclass(SelectTargetPipelineContinuation::class)
        subclass(CycleDrawContinuation::class)
        subclass(TypecycleSearchContinuation::class)
        subclass(DistributeCountersContinuation::class)
    }

    // Component hierarchy (for GameState persistence)
    polymorphic(Component::class) {
        // Identity components
        subclass(CardComponent::class)
        subclass(OwnerComponent::class)
        subclass(ControllerComponent::class)
        subclass(PlayerComponent::class)
        subclass(LifeTotalComponent::class)
        subclass(TokenComponent::class)
        subclass(FaceDownComponent::class)
        subclass(RevealedToComponent::class)
        subclass(MorphDataComponent::class)
        subclass(TextReplacementComponent::class)
        subclass(ProtectionComponent::class)
        subclass(CopyOfComponent::class)
        subclass(ChosenColorComponent::class)
        subclass(ChosenCreatureTypeComponent::class)

        // Battlefield components
        subclass(TappedComponent::class)
        subclass(SummoningSicknessComponent::class)
        subclass(CountersComponent::class)
        subclass(DamageComponent::class)
        subclass(AttachedToComponent::class)
        subclass(AttachmentsComponent::class)
        subclass(EnteredThisTurnComponent::class)
        subclass(TimestampComponent::class)

        // Combat components
        subclass(AttackingComponent::class)
        subclass(BlockingComponent::class)
        subclass(BlockedComponent::class)
        subclass(DamageAssignmentComponent::class)
        subclass(DamageAssignmentOrderComponent::class)
        subclass(DealtFirstStrikeDamageComponent::class)
        subclass(RequiresManualDamageAssignmentComponent::class)
        subclass(AttackersDeclaredThisCombatComponent::class)
        subclass(BlockersDeclaredThisCombatComponent::class)

        // Player components
        subclass(ManaPoolComponent::class)
        subclass(LandDropsComponent::class)
        subclass(MulliganStateComponent::class)
        subclass(SkipCombatPhasesComponent::class)
        subclass(SkipUntapComponent::class)
        subclass(PlayerLostComponent::class)
        subclass(LoseAtEndStepComponent::class)

        // Stack components
        subclass(SpellOnStackComponent::class)
        subclass(TriggeredAbilityOnStackComponent::class)
        subclass(ActivatedAbilityOnStackComponent::class)
        subclass(AbilityOnStackComponent::class)
        subclass(TargetsComponent::class)
        subclass(SpellContextComponent::class)

        // Continuous effects
        subclass(ContinuousEffectSourceComponent::class)
    }
}

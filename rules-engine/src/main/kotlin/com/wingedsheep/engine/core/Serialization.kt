package com.wingedsheep.engine.core

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
}

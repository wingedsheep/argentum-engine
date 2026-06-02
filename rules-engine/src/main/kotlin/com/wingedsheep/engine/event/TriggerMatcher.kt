package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.BecomesTargetEvent
import com.wingedsheep.engine.core.AbilityActivatedEvent
import com.wingedsheep.engine.core.AbilityTriggeredEvent
import com.wingedsheep.engine.core.AttackersDeclaredEvent
import com.wingedsheep.engine.core.BlockersDeclaredEvent
import com.wingedsheep.engine.core.CardCycledEvent
import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.GiftGivenEvent
import com.wingedsheep.engine.core.RoomFullyUnlockedEvent
import com.wingedsheep.engine.core.CardRevealedFromDrawEvent
import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.CommitCrimeEvent
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.state.components.player.ManaSpentOnSpellsThisTurnComponent
import com.wingedsheep.engine.core.LandTappedForManaEvent
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.TransformedEvent
import com.wingedsheep.engine.core.TurnFaceUpEvent
import com.wingedsheep.engine.core.UntappedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.events.SourceFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent

/**
 * Contains shared trigger matching and predicate logic used by all detectors.
 */
class TriggerMatcher(
    private val predicateEvaluator: PredicateEvaluator,
    private val conditionEvaluator: ConditionEvaluator
) {

    fun matchesTrigger(
        trigger: EventPattern,
        binding: TriggerBinding,
        event: EngineGameEvent,
        sourceId: EntityId,
        controllerId: EntityId,
        state: GameState
    ): Boolean {
        // ATTACHED triggers are handled by AttachmentTriggerDetector, not the main loop
        if (binding == TriggerBinding.ATTACHED) return false

        return when (trigger) {
            is EventPattern.ZoneChangeEvent -> matchesZoneChangeTrigger(trigger, binding, event, sourceId, controllerId, state)
            is EventPattern.DrawEvent -> {
                event is CardsDrawnEvent && matchesPlayer(trigger.player, event.playerId, controllerId)
            }
            is EventPattern.CardRevealedFromDrawEvent -> {
                if (event !is CardRevealedFromDrawEvent) return false
                if (event.playerId != controllerId) return false
                val filter = trigger.cardFilter
                if (filter != null) {
                    val requiresCreature = filter.cardPredicates.any {
                        it is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature
                    }
                    if (requiresCreature && !event.isCreature) return false
                }
                true
            }
            is EventPattern.AttackEvent -> {
                event is AttackersDeclaredEvent &&
                    checkBinding(binding, sourceId, event.attackers) &&
                    trigger.requires.all { matchesAttackPredicate(it, event) }
            }
            is EventPattern.YouAttackEvent -> {
                if (event !is AttackersDeclaredEvent) return false
                if (state.activePlayerId != controllerId) return false
                val filter = trigger.attackerFilter
                if (filter != null) {
                    // Count attackers matching the filter
                    val predicateEvaluator = PredicateEvaluator()
                    val projected = state.projectedState
                    val predicateContext = com.wingedsheep.engine.handlers.PredicateContext(
                        controllerId = controllerId,
                        sourceId = sourceId
                    )
                    val matchingCount = event.attackers.count { attackerId ->
                        predicateEvaluator.matches(state, projected, attackerId, filter, predicateContext)
                    }
                    matchingCount >= trigger.minAttackers
                } else {
                    event.attackers.size >= trigger.minAttackers
                }
            }
            is EventPattern.CreaturesAttackYouEvent -> {
                if (event !is AttackersDeclaredEvent) return false
                // Only count attackers declared against the player themself, not against
                // a planeswalker they control (per CR 509.1b / Orim's Prayer ruling).
                val attackingThisPlayer = event.attackers.count { attackerId ->
                    val attackingComponent = state.getEntity(attackerId)
                        ?.get<com.wingedsheep.engine.state.components.combat.AttackingComponent>()
                    attackingComponent?.defenderId == controllerId
                }
                attackingThisPlayer >= trigger.minAttackers
            }
            is EventPattern.BlockEvent -> {
                event is BlockersDeclaredEvent &&
                    (binding != TriggerBinding.SELF || event.blockers.keys.contains(sourceId))
            }
            is EventPattern.BecomesBlockedEvent -> {
                event is BlockersDeclaredEvent &&
                    (binding != TriggerBinding.SELF || event.blockers.values.any { it.contains(sourceId) })
            }
            is EventPattern.BecomesUnblockedEvent -> {
                // CR 509.3g: fires for an attacker with no creatures declared as blockers.
                // We piggyback on BlockersDeclaredEvent (emitted once at end of
                // declare-blockers): SELF matches when sourceId is an attacker this combat
                // AND is absent from every blocker's blocked-attackers list.
                if (event !is BlockersDeclaredEvent) return false
                if (binding != TriggerBinding.SELF) return false
                val isAttacking = state.getEntity(sourceId)
                    ?.get<com.wingedsheep.engine.state.components.combat.AttackingComponent>() != null
                isAttacking && event.blockers.values.none { it.contains(sourceId) }
            }
            is EventPattern.StateConditionMetEvent -> {
                // Synthetic — never matched against real events. State triggers fire via
                // StateTriggerPoller which produces PendingTriggers directly.
                false
            }
            is EventPattern.BlocksOrBecomesBlockedByEvent -> {
                // Basic match: it's a BlockersDeclaredEvent and the source is involved in combat
                // Per-partner trigger creation happens in detectTriggersForEvent
                if (event !is BlockersDeclaredEvent) return false
                if (binding != TriggerBinding.SELF) return false
                // Source is a blocker or an attacker that's being blocked
                event.blockers.keys.contains(sourceId) ||
                    event.blockers.values.any { it.contains(sourceId) }
            }
            is EventPattern.DealsDamageEvent -> {
                // SELF-bound DealsDamageEvent handled separately in detectDamageSourceTriggers
                // Non-self (observer triggers like "whenever a creature deals damage to you") handled in
                // detectDamageToControllerTriggers and detectSubtypeDamageToPlayerTriggers
                false
            }
            is EventPattern.DamageReceivedEvent -> {
                // Generic (source=Any) DamageReceivedEvent can match in the main loop
                // Specific source-filtered ones are handled in detectDamagedBySourceTriggers
                if (trigger.source != SourceFilter.Any) return false
                event is DamageDealtEvent && (binding != TriggerBinding.SELF || event.targetId == sourceId)
            }
            is EventPattern.SpellCastEvent -> {
                event is SpellCastEvent &&
                    matchesPlayer(trigger.player, event.casterId, controllerId) &&
                    matchesSpellFilter(trigger.spellFilter, event, state, sourceId) &&
                    trigger.requires.all { matchesSpellCastPredicate(it, event, state) }
            }
            is EventPattern.NthSpellCastEvent -> {
                // Fires on SpellCastEvent when the casting player's per-turn spell count
                // reaches exactly the specified threshold (e.g., 2 for "second spell").
                if (event !is SpellCastEvent) return false
                if (!matchesPlayer(trigger.player, event.casterId, controllerId)) return false
                val currentCount = state.playerSpellsCastThisTurn[event.casterId] ?: 0
                currentCount == trigger.nthSpell
            }
            is EventPattern.CastThisSpellEvent -> {
                // "When you cast this spell" — fires on the spell's own cast while on the stack.
                // detectSelfCastTriggers only invokes the matcher for the spell being cast, so
                // this just confirms the event is that very spell's cast (intervening-if, if any,
                // is enforced separately by filterByTriggerCondition per CR 603.4).
                event is SpellCastEvent && event.spellEntityId == sourceId
            }
            is EventPattern.ExpendEvent -> {
                // Expend triggers when cumulative mana spent on spells this turn
                // crosses the threshold. Fires on SpellCastEvent only.
                // Detects the "crossing": previous total < threshold <= new total.
                if (event !is SpellCastEvent) return false
                if (!matchesPlayer(trigger.player, event.casterId, controllerId)) return false

                val playerEntity = state.getEntity(event.casterId) ?: return false
                val manaComponent = playerEntity.get<ManaSpentOnSpellsThisTurnComponent>()
                    ?: return false
                val currentTotal = manaComponent.totalSpent
                val spentThisCast = event.totalManaSpent
                val previousTotal = currentTotal - spentThisCast
                // Trigger if we just crossed the threshold with this cast
                previousTotal < trigger.threshold && currentTotal >= trigger.threshold
            }
            is EventPattern.SpellOrAbilityOnStackEvent -> {
                // Intervening-if: only trigger if the spell/ability has a single target
                val stackEntityId = when (event) {
                    is SpellCastEvent -> event.spellEntityId
                    is AbilityActivatedEvent -> event.abilityEntityId
                    is AbilityTriggeredEvent -> event.abilityEntityId
                    else -> null
                }
                if (stackEntityId == null) return false
                val targets = state.getEntity(stackEntityId)
                    ?.get<TargetsComponent>()?.targets
                targets != null && targets.size == 1
            }
            is EventPattern.AbilityActivatedEvent -> {
                // The engine only emits AbilityActivatedEvent for non-mana activated abilities
                // (mana abilities resolve without the stack), so this naturally matches
                // "activates an ability that isn't a mana ability". Loyalty abilities qualify.
                event is AbilityActivatedEvent &&
                    matchesPlayer(trigger.player, event.controllerId, controllerId)
            }
            is EventPattern.CycleEvent -> {
                event is CardCycledEvent &&
                    matchesPlayer(trigger.player, event.playerId, controllerId) &&
                    (binding != TriggerBinding.SELF || event.cardId == sourceId)
            }
            is EventPattern.GiftGivenEvent -> {
                event is GiftGivenEvent &&
                    matchesPlayer(trigger.player, event.controllerId, controllerId)
            }
            is EventPattern.CommitCrimeEvent -> {
                event is CommitCrimeEvent &&
                    matchesPlayer(trigger.player, event.playerId, controllerId)
            }
            is EventPattern.TargetsChosenEvent -> {
                event is com.wingedsheep.engine.core.TargetsChosenEvent &&
                    matchesPlayer(trigger.player, event.chooserId, controllerId)
            }
            is EventPattern.RoomFullyUnlockedEvent -> {
                event is RoomFullyUnlockedEvent &&
                    matchesPlayer(trigger.player, event.controllerId, controllerId)
            }
            is EventPattern.DoorUnlockedEvent -> {
                // Face-scoped "When you unlock this door" triggers (CR 709.5h) are handled
                // by TriggerDetector.detectDoorUnlockedTriggers, which knows the source face
                // of each face-script ability. Returning false here keeps the regular index
                // loop from firing them on the wrong face.
                false
            }
            is EventPattern.TapEvent -> {
                if (event !is TappedEvent) return false
                if (binding == TriggerBinding.SELF && event.entityId != sourceId) return false
                val filter = trigger.filter
                if (filter != null) {
                    val predicateEvaluator = PredicateEvaluator()
                    val predicateContext = com.wingedsheep.engine.handlers.PredicateContext(
                        controllerId = controllerId,
                        sourceId = sourceId
                    )
                    predicateEvaluator.matches(
                        state, state.projectedState, event.entityId, filter, predicateContext
                    )
                } else true
            }
            is EventPattern.UntapEvent -> {
                event is UntappedEvent && (binding != TriggerBinding.SELF || event.entityId == sourceId)
            }
            is EventPattern.LandTappedForMana -> {
                if (event !is LandTappedForManaEvent) return false
                if (binding == TriggerBinding.SELF && event.landId != sourceId) return false
                if (!matchesPlayer(trigger.player, event.tapperId, controllerId)) return false
                val filter = trigger.landFilter
                if (filter != null) {
                    val predicateEvaluator = PredicateEvaluator()
                    val predicateContext = com.wingedsheep.engine.handlers.PredicateContext(
                        controllerId = controllerId,
                        sourceId = sourceId
                    )
                    predicateEvaluator.matches(
                        state, state.projectedState, event.landId, filter, predicateContext
                    )
                } else true
            }
            is EventPattern.LifeGainEvent -> {
                event is LifeChangedEvent &&
                    event.reason == com.wingedsheep.engine.core.LifeChangeReason.LIFE_GAIN &&
                    matchesPlayer(trigger.player, event.playerId, controllerId)
            }
            is EventPattern.RingTemptedEvent -> {
                event is com.wingedsheep.engine.core.RingTemptedEvent &&
                    matchesPlayer(trigger.player, event.playerId, controllerId)
            }
            is EventPattern.ScriedEvent -> {
                event is com.wingedsheep.engine.core.ScriedEvent &&
                    matchesPlayer(trigger.player, event.playerId, controllerId)
            }
            is EventPattern.BecomesTargetEvent -> {
                event is BecomesTargetEvent && matchesBecomesTargetTrigger(trigger, binding, event, sourceId, controllerId, state)
            }
            is EventPattern.TurnFaceUpEvent -> {
                event is TurnFaceUpEvent && (binding != TriggerBinding.SELF || event.entityId == sourceId)
            }
            is EventPattern.CreatureTurnedFaceUpEvent -> {
                if (event !is TurnFaceUpEvent) return false
                matchesPlayer(trigger.player, event.controllerId, controllerId)
            }
            is EventPattern.TransformEvent -> {
                if (event !is TransformedEvent) return false
                // SELF binding must match the transforming permanent
                if (binding == TriggerBinding.SELF && event.entityId != sourceId) return false
                // intoBackFace==null matches any transform; true/false filters by direction
                val directionMatches = trigger.intoBackFace == null ||
                    trigger.intoBackFace == event.intoBackFace
                directionMatches
            }
            // These are handled separately in their own detect* methods
            is EventPattern.ControlChangeEvent -> false
            // Phase/step triggers are handled separately
            is EventPattern.StepEvent -> false
            // Creature-dealt-damage-by-source-dies triggers are handled separately
            is EventPattern.CreatureDealtDamageBySourceDiesEvent -> false
            // "When damage is prevented this way" fires only via its linked delayed trigger
            // (detectEventBasedDelayedTriggers), never as a battlefield trigger.
            is EventPattern.DamagePreventedEvent -> false
            // Replacement-effect-only events never match as triggers
            is EventPattern.DamageEvent -> false
            is EventPattern.CounterPlacementEvent -> false
            is EventPattern.TokenCreationEvent -> false
            is EventPattern.LifeLossEvent -> {
                event is LifeChangedEvent &&
                    event.reason != com.wingedsheep.engine.core.LifeChangeReason.LIFE_GAIN &&
                    event.oldLife > event.newLife &&
                    matchesPlayer(trigger.player, event.playerId, controllerId)
            }
            is EventPattern.LifeGainOrLossEvent -> {
                event is LifeChangedEvent &&
                    event.oldLife != event.newLife &&
                    matchesPlayer(trigger.player, event.playerId, controllerId)
            }
            is EventPattern.DiscardEvent -> {
                event is CardsDiscardedEvent &&
                    matchingDiscardCount(trigger, event, sourceId, controllerId, state) > 0
            }
            is EventPattern.SearchLibraryEvent -> false
            // ExtraTurnEvent is only used as a replacement effect filter, not a trigger
            is EventPattern.ExtraTurnEvent -> false
            // Batching trigger — handled in detectLibraryToGraveyardBatchTriggers
            is EventPattern.CardsPutIntoGraveyardFromLibraryEvent -> false
            // Batching trigger handled separately by detectAnyToGraveyardBatchTriggers
            is EventPattern.CardsPutIntoYourGraveyardEvent -> false
            // Leave-graveyard batch triggers are handled by detectCardsLeftGraveyardBatchTriggers
            is EventPattern.CardsLeftYourGraveyardEvent -> false
            // Sacrifice batch triggers are handled by detectSacrificeBatchTriggers
            is EventPattern.PermanentsSacrificedEvent -> false
            // Combat damage batch triggers are handled by detectCombatDamageBatchTriggers
            is EventPattern.OneOrMoreDealCombatDamageToPlayerEvent -> false
            // Leave battlefield without dying batch triggers are handled by detectLeaveBattlefieldWithoutDyingBatchTriggers
            is EventPattern.LeaveBattlefieldWithoutDyingEvent -> false
            // Enter battlefield batch triggers are handled by detectPermanentsEnteredBatchTriggers
            is EventPattern.PermanentsEnteredEvent -> false
            is EventPattern.CountersPlacedEvent -> {
                if (event !is CountersAddedEvent) return false
                // Counters.ANY is the wildcard "counters of any type" sentinel.
                if (trigger.counterType != com.wingedsheep.sdk.core.Counters.ANY &&
                    !counterTypesMatch(trigger.counterType, event.counterType)) return false
                // "First time counters this turn" intervening-if (Stalwart Successor).
                if (trigger.firstTimeEachTurn && !event.firstThisTurn) return false
                // Check filter: the permanent receiving counters must match
                if (trigger.filter != GameObjectFilter.Any) {
                    val projected = state.projectedState
                    val predicateContext = com.wingedsheep.engine.handlers.PredicateContext(
                        controllerId = controllerId,
                        sourceId = sourceId
                    )
                    val predicateEvaluator = PredicateEvaluator()
                    if (!predicateEvaluator.matches(state, projected, event.entityId, trigger.filter, predicateContext)) {
                        return false
                    }
                }
                true
            }
        }
    }

    /**
     * How many of the discarded cards satisfy this discard trigger — i.e. how many times the
     * ability fires. Discarding N cards in one resolution fires "whenever ... discards a card"
     * N times (one per card); a [EventPattern.DiscardEvent.cardFilter] narrows that to the matching
     * cards only. Returns 0 when the discarding player doesn't match the trigger's selector, so
     * the boolean "does it trigger?" question is just `matchingDiscardCount(...) > 0`.
     *
     * The filter is evaluated against post-discard state — the cards are already in the graveyard.
     * Safe for zone-independent predicates (type/subtype/color); a filter depending on hand-specific
     * state would read the wrong zone.
     */
    fun matchingDiscardCount(
        trigger: EventPattern.DiscardEvent,
        event: CardsDiscardedEvent,
        sourceId: EntityId,
        controllerId: EntityId,
        state: GameState
    ): Int {
        if (!matchesPlayer(trigger.player, event.playerId, controllerId)) return 0
        val filter = trigger.cardFilter ?: return event.cardIds.size
        val projected = state.projectedState
        val predicateContext = com.wingedsheep.engine.handlers.PredicateContext(
            controllerId = controllerId,
            sourceId = sourceId
        )
        return event.cardIds.count { cardId ->
            predicateEvaluator.matches(state, projected, cardId, filter, predicateContext)
        }
    }

    fun matchesZoneChangeTrigger(
        trigger: EventPattern.ZoneChangeEvent,
        binding: TriggerBinding,
        event: EngineGameEvent,
        sourceId: EntityId,
        controllerId: EntityId,
        state: GameState
    ): Boolean {
        if (event !is ZoneChangeEvent) return false

        // Match zones
        if (trigger.from != null && event.fromZone != trigger.from) return false
        if (trigger.to != null && event.toZone != trigger.to) return false
        if (trigger.excludeTo != null && event.toZone == trigger.excludeTo) return false

        // Check binding
        when (binding) {
            TriggerBinding.SELF -> if (event.entityId != sourceId) return false
            TriggerBinding.OTHER -> if (event.entityId == sourceId) return false
            TriggerBinding.ANY -> { /* no entity restriction */ }
            TriggerBinding.ATTACHED -> return false // handled by AttachmentTriggerDetector
        }

        // Check filter
        if (trigger.filter != GameObjectFilter.Any) {
            val projected = state.projectedState
            // Check card predicates (creature type, subtype, etc.)
            // Note: entity may not exist in state if it was a token cleaned up by SBAs.
            // In that case, fall back to lastKnownTypeLine from the event.
            val entity = state.getEntity(event.entityId)
            val cardComponent = entity?.get<CardComponent>()
            // For permanents leaving the battlefield, prefer lastKnownTypeLine since it
            // captures the projected types/subtypes at the moment of leaving (e.g., a creature
            // that was a Food artifact only because of Ygra's continuous effect). The base
            // cardComponent.typeLine in the new zone has only the printed types.
            val typeLine = if (event.fromZone == Zone.BATTLEFIELD && event.lastKnownTypeLine != null) {
                event.lastKnownTypeLine
            } else {
                cardComponent?.typeLine ?: event.lastKnownTypeLine
            }
            val isFaceDown = entity?.has<FaceDownComponent>() == true

            for (predicate in trigger.filter.cardPredicates) {
                when (predicate) {
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature -> {
                        // For dying creatures: use base state (they're already in graveyard)
                        // Face-down permanents are 2/2 creatures (Rule 708.2) and count.
                        val isCreature = isFaceDown || typeLine?.isCreature == true
                        if (!isCreature) return false
                    }
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsLand -> {
                        if (typeLine?.isLand != true) return false
                    }
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsArtifact -> {
                        if (typeLine?.isArtifact != true) return false
                    }
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsEnchantment -> {
                        if (typeLine?.isEnchantment != true) return false
                    }
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasSubtype -> {
                        // For entering creatures: use projected state (they're on battlefield)
                        // For dying creatures: use base state (they're in graveyard, no projected subtypes)
                        if (event.toZone == Zone.BATTLEFIELD) {
                            if (!projected.hasSubtype(event.entityId, predicate.subtype.value)) return false
                        } else {
                            if (isFaceDown) return false
                            if (typeLine == null) return false
                            if (!typeLine.hasSubtype(predicate.subtype)) return false
                        }
                    }
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasKeyword -> {
                        // For entering creatures: use projected state (they're on battlefield)
                        // For dying/leaving creatures: use lastKnownKeywords from the event since
                        // the creature is no longer on the battlefield and projected state won't
                        // have its keywords (e.g., Jackdaw Savior: "whenever a creature you control
                        // with flying dies").
                        if (event.fromZone == Zone.BATTLEFIELD && event.lastKnownKeywords.isNotEmpty()) {
                            if (predicate.keyword.name !in event.lastKnownKeywords) return false
                        } else {
                            if (!projected.hasKeyword(event.entityId, predicate.keyword)) return false
                        }
                    }
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasChosenSubtype -> {
                        // Resolve the chosen subtype from the trigger source's ChosenCreatureTypeComponent
                        // and check that the entering/leaving entity has that subtype.
                        val chosenType = state.getEntity(sourceId)
                            ?.get<ChosenCreatureTypeComponent>()?.creatureType
                            ?: return false
                        val hasSubtype = if (event.toZone == Zone.BATTLEFIELD) {
                            projected.hasSubtype(event.entityId, chosenType)
                        } else {
                            typeLine?.subtypes?.any { it.value.equals(chosenType, ignoreCase = true) } == true
                        }
                        val isChangelingCreatureType = cardComponent != null &&
                            Keyword.CHANGELING in cardComponent.baseKeywords &&
                            chosenType in Subtype.ALL_CREATURE_TYPES
                        if (!hasSubtype && !isChangelingCreatureType) return false
                    }
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNontoken -> {
                        // Token-ness is intrinsic; LKI is required because 704.5d sweeps the
                        // token entity before the matcher runs on death triggers, and the
                        // entity may also be unreachable after a bounce/exile by the time
                        // we check. For enter-battlefield events the entity is still in state
                        // and lastKnownWasToken is not populated — read TokenComponent live.
                        val isToken = if (event.fromZone == Zone.BATTLEFIELD) {
                            event.lastKnownWasToken
                        } else {
                            entity?.has<com.wingedsheep.engine.state.components.identity.TokenComponent>() == true
                        }
                        if (isToken) return false
                    }
                    is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsToken -> {
                        val isToken = if (event.fromZone == Zone.BATTLEFIELD) {
                            event.lastKnownWasToken
                        } else {
                            entity?.has<com.wingedsheep.engine.state.components.identity.TokenComponent>() == true
                        }
                        if (!isToken) return false
                    }
                    else -> {
                        // For other predicates, check the entity's type
                        if (cardComponent == null) return false
                        if (!matchesCardPredicate(
                                predicate, cardComponent, projected, event.entityId, isFaceDown,
                                lastKnownPower = event.lastKnownPower,
                                lastKnownToughness = event.lastKnownToughness,
                                lastKnownWasToken = event.lastKnownWasToken
                            )) return false
                    }
                }
            }
            // Check state predicates (face-down, tapped, etc.)
            for (predicate in trigger.filter.statePredicates) {
                if (!matchesStatePredicateForZoneChangeTrigger(predicate, state, event)) return false
            }
            // Check controller predicate. Control predicates (youControl) prefer the dying
            // creature's last-known controller over its owner so stolen creatures (Threaten /
            // Mind Control) and tokens whose creator owns them but doesn't control them group
            // correctly. Owner predicates (ownedByYou) instead read the card's owner: "put into
            // YOUR graveyard from the battlefield" cares about ownership, since a permanent
            // always goes to its owner's graveyard regardless of who last controlled it
            // (CR 400.3, e.g. Soulcatchers' Aerie).
            if (trigger.filter.controllerPredicate != null) {
                val effectiveController = event.lastKnownController ?: event.ownerId
                when (trigger.filter.controllerPredicate) {
                    is com.wingedsheep.sdk.scripting.predicates.ControllerPredicate.ControlledByYou -> {
                        if (effectiveController != controllerId) return false
                    }
                    is com.wingedsheep.sdk.scripting.predicates.ControllerPredicate.ControlledByOpponent -> {
                        if (effectiveController == controllerId) return false
                    }
                    is com.wingedsheep.sdk.scripting.predicates.ControllerPredicate.OwnedByYou -> {
                        if (event.ownerId != controllerId) return false
                    }
                    is com.wingedsheep.sdk.scripting.predicates.ControllerPredicate.OwnedByOpponent -> {
                        if (event.ownerId == controllerId) return false
                    }
                    else -> {}
                }
            }
        }

        return true
    }

    fun matchesCardPredicate(
        predicate: com.wingedsheep.sdk.scripting.predicates.CardPredicate,
        cardComponent: CardComponent,
        projected: ProjectedState,
        entityId: EntityId,
        isFaceDown: Boolean = false,
        lastKnownPower: Int? = null,
        lastKnownToughness: Int? = null,
        /**
         * Token-ness for zone-change triggers where the entity may already be gone
         * (CR 704.5s sweeps tokens out of non-battlefield zones). Pass [ZoneChangeEvent.lastKnownWasToken]
         * so `IsNontoken` / `IsToken` can still resolve. When `null`, the token bit is read
         * from the base state via the projected snapshot (entity must still exist).
         */
        lastKnownWasToken: Boolean? = null
    ): Boolean {
        fun isToken(): Boolean {
            if (lastKnownWasToken != null) return lastKnownWasToken
            val entity = projected.getBaseState().getEntity(entityId) ?: return false
            return entity.has<com.wingedsheep.engine.state.components.identity.TokenComponent>()
        }
        return when (predicate) {
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature -> cardComponent.typeLine.isCreature
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsLand -> cardComponent.typeLine.isLand
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsArtifact -> cardComponent.typeLine.isArtifact
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsEnchantment -> cardComponent.typeLine.isEnchantment
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsPlaneswalker -> com.wingedsheep.sdk.core.CardType.PLANESWALKER in cardComponent.typeLine.cardTypes
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsInstant -> cardComponent.typeLine.isInstant
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsSorcery -> cardComponent.typeLine.isSorcery
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsBasicLand -> cardComponent.typeLine.isLand && cardComponent.typeLine.supertypes.contains(com.wingedsheep.sdk.core.Supertype.BASIC)
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsPermanent -> cardComponent.typeLine.isPermanent
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNonland -> !cardComponent.typeLine.isLand
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNoncreature -> !cardComponent.typeLine.isCreature
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNonenchantment -> !cardComponent.typeLine.isEnchantment
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsLegendary -> cardComponent.typeLine.isLegendary
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNonlegendary -> !cardComponent.typeLine.isLegendary
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasSubtype ->
                cardComponent.typeLine.hasSubtype(predicate.subtype)
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasAnyOfSubtypes ->
                predicate.subtypes.any { cardComponent.typeLine.hasSubtype(it) }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueAtLeast -> {
                val cmc = if (isFaceDown) 0 else cardComponent.manaValue
                cmc >= predicate.min
            }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueAtMost -> {
                val cmc = if (isFaceDown) 0 else cardComponent.manaValue
                cmc <= predicate.max
            }
            com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueAtMostX -> false
            // Resolution-time chosen-number predicate; TriggerMatcher has no chosen-number context.
            com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueEqualsX -> false
            // Entity-relative — TriggerMatcher has no entity context; predicate doesn't apply here.
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueAtMostEntity -> false
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueAtMostEntityManaSpent -> false
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.PowerGreaterThanEntity -> false
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueEquals -> {
                val cmc = if (isFaceDown) 0 else cardComponent.manaValue
                cmc == predicate.value
            }
            com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueIsEven -> {
                val cmc = if (isFaceDown) 0 else cardComponent.manaValue
                cmc % 2 == 0
            }
            com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueIsOdd -> {
                val cmc = if (isFaceDown) 0 else cardComponent.manaValue
                cmc % 2 != 0
            }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.PowerAtLeast -> {
                val power = if (isFaceDown) 2
                    else lastKnownPower ?: projected.getPower(entityId) ?: cardComponent.baseStats?.basePower ?: 0
                power >= predicate.min
            }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.PowerAtMost -> {
                val power = if (isFaceDown) 2
                    else lastKnownPower ?: projected.getPower(entityId) ?: cardComponent.baseStats?.basePower ?: 0
                power <= predicate.max
            }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.PowerEquals -> {
                val power = if (isFaceDown) 2
                    else lastKnownPower ?: projected.getPower(entityId) ?: cardComponent.baseStats?.basePower ?: 0
                power == predicate.value
            }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.ToughnessAtLeast -> {
                val toughness = if (isFaceDown) 2
                    else lastKnownToughness ?: projected.getToughness(entityId) ?: cardComponent.baseStats?.baseToughness ?: 0
                toughness >= predicate.min
            }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.ToughnessAtMost -> {
                val toughness = if (isFaceDown) 2
                    else lastKnownToughness ?: projected.getToughness(entityId) ?: cardComponent.baseStats?.baseToughness ?: 0
                toughness <= predicate.max
            }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.ToughnessEquals -> {
                val toughness = if (isFaceDown) 2
                    else lastKnownToughness ?: projected.getToughness(entityId) ?: cardComponent.baseStats?.baseToughness ?: 0
                toughness == predicate.value
            }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.PowerOrToughnessAtLeast -> {
                val power = if (isFaceDown) 2
                    else lastKnownPower ?: projected.getPower(entityId) ?: cardComponent.baseStats?.basePower ?: 0
                val toughness = if (isFaceDown) 2
                    else lastKnownToughness ?: projected.getToughness(entityId) ?: cardComponent.baseStats?.baseToughness ?: 0
                power >= predicate.min || toughness >= predicate.min
            }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.TotalPowerAndToughnessAtMost -> {
                val power = if (isFaceDown) 2
                    else lastKnownPower ?: projected.getPower(entityId) ?: cardComponent.baseStats?.basePower ?: 0
                val toughness = if (isFaceDown) 2
                    else lastKnownToughness ?: projected.getToughness(entityId) ?: cardComponent.baseStats?.baseToughness ?: 0
                (power + toughness) <= predicate.max
            }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.ToughnessGreaterThanPower -> {
                val power = if (isFaceDown) 2
                    else lastKnownPower ?: projected.getPower(entityId) ?: cardComponent.baseStats?.basePower ?: 0
                val toughness = if (isFaceDown) 2
                    else lastKnownToughness ?: projected.getToughness(entityId) ?: cardComponent.baseStats?.baseToughness ?: 0
                toughness > power
            }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasKeyword ->
                projected.hasKeyword(entityId, predicate.keyword)
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.NotKeyword ->
                !projected.hasKeyword(entityId, predicate.keyword)
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsToken -> isToken()
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNontoken -> !isToken()
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.Or ->
                predicate.predicates.any { matchesCardPredicate(it, cardComponent, projected, entityId, isFaceDown, lastKnownPower, lastKnownToughness, lastKnownWasToken) }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.And ->
                predicate.predicates.all { matchesCardPredicate(it, cardComponent, projected, entityId, isFaceDown, lastKnownPower, lastKnownToughness, lastKnownWasToken) }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.Not ->
                !matchesCardPredicate(predicate.predicate, cardComponent, projected, entityId, isFaceDown, lastKnownPower, lastKnownToughness, lastKnownWasToken)
            else -> error(
                "TriggerMatcher.matchesCardPredicate has no branch for ${predicate::class.simpleName}. " +
                "Add an explicit branch — silent pass-through hid the IsNontoken bug for months."
            )
        }
    }

    /**
     * Check if a trigger event matches a death trigger pattern (ZoneChangeEvent from battlefield to graveyard).
     */
    fun isDeathTrigger(trigger: EventPattern): Boolean {
        return trigger is EventPattern.ZoneChangeEvent &&
            trigger.from == Zone.BATTLEFIELD &&
            trigger.to == Zone.GRAVEYARD
    }

    /**
     * Check if a trigger event matches a leaves-battlefield pattern (ZoneChangeEvent from battlefield, to=null).
     */
    fun isLeavesBattlefieldTrigger(trigger: EventPattern): Boolean {
        return trigger is EventPattern.ZoneChangeEvent &&
            trigger.from == Zone.BATTLEFIELD &&
            trigger.to == null
    }

    /**
     * Check if a trigger fires for a specific battlefield-exit destination
     * (e.g., "When this is exiled from the battlefield" → trigger.to = EXILE).
     *
     * Excludes [Zone.GRAVEYARD] — death triggers are handled by [isDeathTrigger]
     * and [DeathAndLeaveTriggerDetector.detectDeathTriggers], so matching here
     * would double-fire. Generic "leaves the battlefield" triggers (to = null)
     * are matched by [isLeavesBattlefieldTrigger] instead.
     */
    fun isLeavesBattlefieldToZoneTrigger(trigger: EventPattern, eventToZone: Zone): Boolean {
        if (eventToZone == Zone.GRAVEYARD) return false
        return trigger is EventPattern.ZoneChangeEvent &&
            trigger.from == Zone.BATTLEFIELD &&
            trigger.to == eventToZone
    }

    /**
     * Check binding for AttackEvent: SELF means sourceId must be in attackers.
     */
    fun checkBinding(binding: TriggerBinding, sourceId: EntityId, entityIds: List<EntityId>): Boolean {
        return when (binding) {
            TriggerBinding.SELF -> sourceId in entityIds
            TriggerBinding.OTHER -> true  // "whenever another creature attacks" (not currently used, but correct)
            TriggerBinding.ANY -> true
            TriggerBinding.ATTACHED -> false // handled by AttachmentTriggerDetector
        }
    }

    /**
     * Check if a Player filter matches the event's player.
     */
    fun matchesPlayer(player: Player, eventPlayerId: EntityId, controllerId: EntityId): Boolean {
        return when (player) {
            Player.You -> eventPlayerId == controllerId
            Player.Each -> true
            Player.Opponent -> eventPlayerId != controllerId
            else -> true
        }
    }

    /**
     * Extract a subtype value from a GameObjectFilter (used for DealsDamageEvent.sourceFilter).
     */
    fun extractSubtypeFromFilter(filter: GameObjectFilter): String? {
        for (predicate in filter.cardPredicates) {
            if (predicate is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasSubtype) {
                return predicate.subtype.value
            }
        }
        return null
    }

    /**
     * Check if a BecomesTargetEvent matches a BecomesTargetEvent trigger.
     * Validates binding (SELF = targeted entity is this permanent) and
     * checks the targetFilter against the targeted entity.
     */
    fun matchesBecomesTargetTrigger(
        trigger: EventPattern.BecomesTargetEvent,
        binding: TriggerBinding,
        event: BecomesTargetEvent,
        sourceId: EntityId,
        controllerId: EntityId,
        state: GameState
    ): Boolean {
        // Check binding: SELF means this permanent was targeted
        when (binding) {
            TriggerBinding.SELF -> if (event.targetEntityId != sourceId) return false
            TriggerBinding.OTHER -> if (event.targetEntityId == sourceId) return false
            TriggerBinding.ANY -> { /* no entity restriction */ }
            TriggerBinding.ATTACHED -> return false // handled by AttachmentTriggerDetector
        }

        // Valiant: check if the targeting spell/ability is controlled by "you" (the trigger's controller)
        if (trigger.byYou && event.controllerId != controllerId) return false

        // Check if the targeting spell/ability is controlled by an opponent
        if (trigger.byOpponent && event.controllerId == controllerId) return false

        // Valiant: check if this is the first time this turn
        if (trigger.firstTimeEachTurn && !event.firstTimeByThisController) return false

        // Check targetFilter against the targeted entity
        if (trigger.targetFilter != GameObjectFilter.Any) {
            val projected = state.projectedState
            val targetContainer = state.getEntity(event.targetEntityId) ?: return false
            val targetCard = targetContainer.get<CardComponent>() ?: return false

            // Check card predicates
            for (predicate in trigger.targetFilter.cardPredicates) {
                if (!matchesCardPredicate(predicate, targetCard, projected, event.targetEntityId)) return false
            }

            // Check controller predicate
            if (trigger.targetFilter.controllerPredicate != null) {
                val targetController = projected.getController(event.targetEntityId) ?: return false
                when (trigger.targetFilter.controllerPredicate) {
                    is com.wingedsheep.sdk.scripting.predicates.ControllerPredicate.ControlledByYou -> {
                        if (targetController != controllerId) return false
                    }
                    is com.wingedsheep.sdk.scripting.predicates.ControllerPredicate.ControlledByOpponent -> {
                        if (targetController == controllerId) return false
                    }
                    else -> {}
                }
            }
        }

        return true
    }

    fun matchesDealsDamageTrigger(
        trigger: EventPattern.DealsDamageEvent,
        event: DamageDealtEvent,
        state: GameState,
        controllerId: EntityId? = null
    ): Boolean {
        val combatMatches = trigger.damageType == DamageType.Any ||
            (trigger.damageType == DamageType.Combat && event.isCombatDamage) ||
            (trigger.damageType == DamageType.NonCombat && !event.isCombatDamage)
        val recipientMatches = when (trigger.recipient) {
            RecipientFilter.Any -> true
            RecipientFilter.AnyPlayer -> event.targetId in state.turnOrder
            RecipientFilter.AnyPlayerOrPlaneswalker -> {
                event.targetId in state.turnOrder ||
                    state.projectedState.isPlaneswalker(event.targetId)
            }
            RecipientFilter.AnyCreature -> event.targetId !in state.turnOrder
            RecipientFilter.You -> false // handled separately in detectDamageToControllerTriggers
            RecipientFilter.Opponent -> {
                event.targetId in state.turnOrder && event.targetId != controllerId
            }
            RecipientFilter.CreatureOpponentControls -> {
                val targetController = recipientControllerLki(event, state)
                recipientIsCreatureLki(event, state) && targetController != null && targetController != controllerId
            }
            RecipientFilter.CreatureYouControl -> {
                recipientIsCreatureLki(event, state) && recipientControllerLki(event, state) == controllerId
            }
            RecipientFilter.PermanentYouControl -> {
                recipientControllerLki(event, state) == controllerId
            }
            RecipientFilter.AnyPermanent -> event.targetId !in state.turnOrder
            RecipientFilter.Self -> false // handled elsewhere
            else -> false
        }
        // Check sourceFilter (e.g., "creature you control" for Gossip's Talent)
        val sourceMatches = if (trigger.sourceFilter != null && event.sourceId != null) {
            val predicateContext = com.wingedsheep.engine.handlers.PredicateContext(
                controllerId = controllerId ?: EntityId(""),
                sourceId = null
            )
            predicateEvaluator.matches(
                state, state.projectedState, event.sourceId, trigger.sourceFilter!!, predicateContext
            )
        } else {
            true
        }
        // "Excess damage" triggers (Fall of Cair Andros) fire only when the recipient took
        // damage past lethal — CR 120.4a. Non-creature targets and at-or-below-lethal hits leave
        // event.excessAmount at 0 and silently fail this gate.
        val excessMatches = !trigger.requireExcess || event.excessAmount > 0
        return combatMatches && recipientMatches && sourceMatches && excessMatches
    }

    /**
     * The damage recipient's controller, preferring live state but falling back to the controller
     * captured at damage time ([DamageDealtEvent.targetControllerId]). A creature destroyed by the
     * same damage event has already moved to the graveyard — losing its [ControllerComponent] — by
     * the time combat-damage triggers are detected, so without the LKI fallback a recipient-based
     * trigger ("a creature you control / an opponent controls is dealt damage") would silently miss
     * the killing blow (CR 603.10).
     */
    private fun recipientControllerLki(event: DamageDealtEvent, state: GameState): EntityId? =
        state.getEntity(event.targetId)?.get<ControllerComponent>()?.playerId
            ?: event.targetControllerId

    /** Whether the damage recipient was a creature, with the same LKI fallback as [recipientControllerLki]. */
    private fun recipientIsCreatureLki(event: DamageDealtEvent, state: GameState): Boolean =
        state.getEntity(event.targetId)?.get<CardComponent>()?.typeLine?.isCreature == true ||
            event.targetWasCreature

    fun matchesStepTrigger(
        trigger: EventPattern,
        step: Step,
        controllerId: EntityId,
        activePlayerId: EntityId
    ): Boolean {
        if (trigger !is EventPattern.StepEvent) return false
        if (step != trigger.step) return false
        return matchesPlayerForStep(trigger.player, controllerId, activePlayerId)
    }

    fun matchesPlayerForStep(player: Player, controllerId: EntityId, activePlayerId: EntityId): Boolean {
        return when (player) {
            Player.You -> controllerId == activePlayerId
            Player.Each -> true
            Player.Opponent, Player.EachOpponent -> controllerId != activePlayerId
            else -> true
        }
    }

    fun matchesSpellFilter(
        spellFilter: GameObjectFilter,
        event: SpellCastEvent,
        state: GameState,
        triggerSourceId: EntityId? = null
    ): Boolean {
        // No card predicates = match any spell (equivalent to old SpellTypeFilter.ANY)
        if (spellFilter.cardPredicates.isEmpty()) return true

        val container = state.getEntity(event.spellEntityId) ?: return false

        // Face-down spells have no characteristics (CR 708.2) — they don't match any type filter
        val isFaceDown = container.get<SpellOnStackComponent>()?.castFaceDown == true
        if (isFaceDown) return false

        // Use base-state matching (spells on the stack don't get continuous effects)
        // Pass sourceId so HasChosenSubtype can read the trigger source's ChosenCreatureTypeComponent
        val context = com.wingedsheep.engine.handlers.PredicateContext(
            controllerId = event.casterId,
            sourceId = triggerSourceId
        )
        return predicateEvaluator.matches(state, state.projectedState, event.spellEntityId, spellFilter, context)
    }

    /**
     * Check whether the spell was cast from the zone required by the trigger
     * (e.g., HAND for "Whenever you cast a spell from your hand"). When the
     * trigger doesn't specify a zone, every cast matches.
     */
    /**
     * Resolve one [SpellCastPredicate] against the runtime [SpellCastEvent].
     *
     * Add a new branch here when extending [SpellCastPredicate] with a new
     * cast-time fact. The matcher is conjunctive — every predicate the
     * trigger declares must hold.
     *
     * Note on [SpellCastPredicate.PaidWithManaFromSubtype]: the SDK exposes
     * any token subtype, but the engine currently only tracks Treasure-sourced
     * mana (via [SpellCastEvent.paidWithTreasureMana] / `ManaPoolComponent.treasureMana`).
     * Other subtypes resolve to `false` so triggers silently don't fire until
     * the mana-pool tracker is generalized to a `Set<Subtype>`; card
     * definitions can already declare them forward-compatibly.
     */
    /**
     * Resolve one [AttackPredicate] against the runtime [AttackersDeclaredEvent].
     *
     * Add a new branch here when extending [AttackPredicate] with a new
     * attack-time fact. The matcher is conjunctive — every predicate the
     * trigger declares must hold.
     */
    private fun matchesAttackPredicate(
        predicate: AttackPredicate,
        event: AttackersDeclaredEvent
    ): Boolean = when (predicate) {
        AttackPredicate.Alone -> event.attackers.size == 1
        is AttackPredicate.AttackerCountAtLeast -> event.attackers.size >= predicate.n
    }

    private fun matchesSpellCastPredicate(
        predicate: SpellCastPredicate,
        event: SpellCastEvent,
        state: GameState
    ): Boolean = when (predicate) {
        is SpellCastPredicate.CastFromZone -> {
            val spellComponent = state.getEntity(event.spellEntityId)?.get<SpellOnStackComponent>()
            spellComponent?.castFromZone == predicate.zone
        }
        SpellCastPredicate.WasKicked -> event.wasKicked
        is SpellCastPredicate.PaidWithManaFromSubtype -> when (predicate.subtype) {
            Subtype.TREASURE -> event.paidWithTreasureMana
            else -> false
        }
        SpellCastPredicate.IsModal -> event.chosenModesCount > 0
    }

    /**
     * Filter out triggers whose intervening-if condition (Rule 603.4) is not met.
     * If a triggered ability has a triggerCondition, it is only allowed to fire
     * when that condition is true at the time of trigger detection.
     */
    fun filterByTriggerCondition(
        state: GameState,
        triggers: List<PendingTrigger>
    ): List<PendingTrigger> {
        return triggers.filter { trigger ->
            val condition = trigger.ability.triggerCondition ?: return@filter true
            val context = EffectContext(
                sourceId = trigger.sourceId,
                controllerId = trigger.controllerId,
                opponentId = state.turnOrder.firstOrNull { it != trigger.controllerId },
                triggeringEntityId = trigger.triggerContext.triggeringEntityId,
                triggerDamageAmount = trigger.triggerContext.damageAmount,
                triggerCounterCount = trigger.triggerContext.counterCount,
                triggerTotalCounterCount = trigger.triggerContext.totalCounterCount,
                triggerMinusOneMinusOneCounterCount = trigger.triggerContext.minusOneMinusOneCounterCount,
                triggerLastKnownPower = trigger.triggerContext.lastKnownPower,
                triggerLastKnownToughness = trigger.triggerContext.lastKnownToughness,
                triggerScryCount = trigger.triggerContext.scryCount,
                triggerExcessDamageAmount = trigger.triggerContext.excessDamageAmount
            )
            conditionEvaluator.evaluate(state, condition, context)
        }
    }

    /**
     * Sort triggers by APNAP order.
     * Active player's triggers go on the stack first (resolve last),
     * then non-active players in turn order.
     */
    fun sortByApnapOrder(
        state: GameState,
        triggers: List<PendingTrigger>
    ): List<PendingTrigger> {
        val activePlayerId = state.activePlayerId ?: return triggers

        // Group by controller
        val byController = triggers.groupBy { it.controllerId }

        // Get player order starting from active player
        val playerOrder = state.turnOrder.let { players ->
            val activeIndex = players.indexOf(activePlayerId)
            if (activeIndex == -1) players
            else players.drop(activeIndex) + players.take(activeIndex)
        }

        // Build result in APNAP order
        // Active player's triggers first (they go on stack first = resolve last)
        return playerOrder.flatMap { playerId ->
            byController[playerId] ?: emptyList()
        }
    }

    /**
     * Evaluate a [StatePredicate] in the zone-change trigger gating path, with last-known
     * info taken from the event so the dying / leaving entity can still be evaluated after
     * it has left the battlefield. Falls back to [matchesStatePredicateForTrigger] for
     * predicates that don't need event-side LKI.
     */
    private fun matchesStatePredicateForZoneChangeTrigger(
        predicate: com.wingedsheep.sdk.scripting.predicates.StatePredicate,
        state: GameState,
        event: ZoneChangeEvent
    ): Boolean = when (predicate) {
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.HasGreatestPower -> {
            // For permanents leaving the battlefield we need the dying entity's power +
            // controller from the event's last-known info (the entity is no longer on the
            // battlefield by the time the trigger gates). For ETB triggers (to=BATTLEFIELD)
            // the entity is live and the projected path applies.
            val leavingBattlefield = event.fromZone == Zone.BATTLEFIELD
            if (leavingBattlefield) {
                val dyingPower = event.lastKnownPower
                val dyingController = event.lastKnownController ?: event.ownerId
                if (dyingPower == null) false
                else {
                    val projected = state.projectedState
                    val maxSurvivorPower = state.getBattlefield()
                        .filter { id ->
                            projected.getController(id) == dyingController && projected.isCreature(id)
                        }
                        .maxOfOrNull { projected.getPower(it) ?: Int.MIN_VALUE }
                    // Singleton case (no survivors under same controller): the dying
                    // creature is trivially its own maximum and qualifies.
                    maxSurvivorPower == null || dyingPower >= maxSurvivorPower
                }
            } else {
                matchesStatePredicateForTrigger(predicate, state, event.entityId)
            }
        }
        is com.wingedsheep.sdk.scripting.predicates.StatePredicate.Or ->
            predicate.predicates.any { matchesStatePredicateForZoneChangeTrigger(it, state, event) }
        is com.wingedsheep.sdk.scripting.predicates.StatePredicate.And ->
            predicate.predicates.all { matchesStatePredicateForZoneChangeTrigger(it, state, event) }
        is com.wingedsheep.sdk.scripting.predicates.StatePredicate.Not ->
            !matchesStatePredicateForZoneChangeTrigger(predicate.predicate, state, event)
        else -> matchesStatePredicateForTrigger(predicate, state, event.entityId)
    }

    private fun matchesStatePredicateForTrigger(
        predicate: com.wingedsheep.sdk.scripting.predicates.StatePredicate,
        state: GameState,
        entityId: EntityId
    ): Boolean = when (predicate) {
        is com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsFaceDown -> {
            val entity = state.getEntity(entityId) ?: return false
            entity.has<FaceDownComponent>()
        }
        is com.wingedsheep.sdk.scripting.predicates.StatePredicate.Or ->
            predicate.predicates.any { matchesStatePredicateForTrigger(it, state, entityId) }
        is com.wingedsheep.sdk.scripting.predicates.StatePredicate.And ->
            predicate.predicates.all { matchesStatePredicateForTrigger(it, state, entityId) }
        is com.wingedsheep.sdk.scripting.predicates.StatePredicate.Not ->
            !matchesStatePredicateForTrigger(predicate.predicate, state, entityId)
        // Trigger-matching predicates beyond IsFaceDown are not currently used as
        // *trigger-gating* filters (those evaluate the triggering entity, not the source
        // state). Returning true preserves the prior "don't gate" behavior, but listing
        // every variant forces a compile-time choice when a new predicate is added.
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsTapped,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsUntapped,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsAttacking,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsBlocking,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsBlocked,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsUnblocked,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.InSameBandAsSource,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.EnteredThisTurn,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.WasDealtDamageThisTurn,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.HasDealtDamage,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.HasDealtCombatDamageToPlayer,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.AttackedThisTurn,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsFaceUp,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.HasMorphAbility,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.HasAnyCounter,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.HasGreatestPower,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsEquipped,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsModified,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsSaddled,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsWarpExiled,
        com.wingedsheep.sdk.scripting.predicates.StatePredicate.WasCastForWarp,
        is com.wingedsheep.sdk.scripting.predicates.StatePredicate.HasCounter -> true
    }

    /**
     * Compare counter type strings, normalizing different representations to allow matching
     * between trigger specs (e.g., "+1/+1") and event strings (which may be "+1/+1",
     * "plus_one_plus_one", "PLUS_ONE_PLUS_ONE", etc.).
     */
    private fun counterTypesMatch(triggerType: String, eventType: String): Boolean {
        if (triggerType == eventType) return true
        return normalizeCounterType(triggerType) == normalizeCounterType(eventType)
    }

    private fun normalizeCounterType(type: String): String =
        type.lowercase()
            .replace("+1/+1", "plus_one_plus_one")
            .replace("-1/-1", "minus_one_minus_one")
            .replace(" ", "_")
}

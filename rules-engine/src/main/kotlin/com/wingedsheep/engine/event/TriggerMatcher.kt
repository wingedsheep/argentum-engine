package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.BecomesTargetEvent
import com.wingedsheep.engine.core.AbilityActivatedEvent
import com.wingedsheep.engine.core.AbilityTriggeredEvent
import com.wingedsheep.engine.core.AttackersDeclaredEvent
import com.wingedsheep.engine.core.BlockersDeclaredEvent
import com.wingedsheep.engine.core.CardCycledEvent
import com.wingedsheep.engine.core.GiftGivenEvent
import com.wingedsheep.engine.core.CardRevealedFromDrawEvent
import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.state.components.player.ManaSpentOnSpellsThisTurnComponent
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.TurnFaceUpEvent
import com.wingedsheep.engine.core.UntappedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.SourceFilter

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
        trigger: GameEvent,
        binding: TriggerBinding,
        event: EngineGameEvent,
        sourceId: EntityId,
        controllerId: EntityId,
        state: GameState
    ): Boolean {
        // ATTACHED triggers are handled by AttachmentTriggerDetector, not the main loop
        if (binding == TriggerBinding.ATTACHED) return false

        return when (trigger) {
            is GameEvent.ZoneChangeEvent -> matchesZoneChangeTrigger(trigger, binding, event, sourceId, controllerId, state)
            is GameEvent.DrawEvent -> {
                event is CardsDrawnEvent && matchesPlayer(trigger.player, event.playerId, controllerId)
            }
            is GameEvent.CardRevealedFromDrawEvent -> {
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
            is GameEvent.AttackEvent -> {
                event is AttackersDeclaredEvent &&
                    checkBinding(binding, sourceId, event.attackers) &&
                    (!trigger.alone || event.attackers.size == 1)
            }
            is GameEvent.YouAttackEvent -> {
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
                        predicateEvaluator.matchesWithProjection(state, projected, attackerId, filter, predicateContext)
                    }
                    matchingCount >= trigger.minAttackers
                } else {
                    event.attackers.size >= trigger.minAttackers
                }
            }
            is GameEvent.BlockEvent -> {
                event is BlockersDeclaredEvent &&
                    (binding != TriggerBinding.SELF || event.blockers.keys.contains(sourceId))
            }
            is GameEvent.BecomesBlockedEvent -> {
                event is BlockersDeclaredEvent &&
                    (binding != TriggerBinding.SELF || event.blockers.values.any { it.contains(sourceId) })
            }
            is GameEvent.BlocksOrBecomesBlockedByEvent -> {
                // Basic match: it's a BlockersDeclaredEvent and the source is involved in combat
                // Per-partner trigger creation happens in detectTriggersForEvent
                if (event !is BlockersDeclaredEvent) return false
                if (binding != TriggerBinding.SELF) return false
                // Source is a blocker or an attacker that's being blocked
                event.blockers.keys.contains(sourceId) ||
                    event.blockers.values.any { it.contains(sourceId) }
            }
            is GameEvent.DealsDamageEvent -> {
                // SELF-bound DealsDamageEvent handled separately in detectDamageSourceTriggers
                // Non-self (observer triggers like "whenever a creature deals damage to you") handled in
                // detectDamageToControllerTriggers and detectSubtypeDamageToPlayerTriggers
                false
            }
            is GameEvent.DamageReceivedEvent -> {
                // Generic (source=Any) DamageReceivedEvent can match in the main loop
                // Specific source-filtered ones are handled in detectDamagedBySourceTriggers
                if (trigger.source != SourceFilter.Any) return false
                event is DamageDealtEvent && (binding != TriggerBinding.SELF || event.targetId == sourceId)
            }
            is GameEvent.SpellCastEvent -> {
                event is SpellCastEvent &&
                    matchesPlayer(trigger.player, event.casterId, controllerId) &&
                    matchesSpellFilter(trigger.spellFilter, event, state) &&
                    (trigger.kicked == null || trigger.kicked == event.wasKicked)
            }
            is GameEvent.NthSpellCastEvent -> {
                // Fires on SpellCastEvent when the casting player's per-turn spell count
                // reaches exactly the specified threshold (e.g., 2 for "second spell").
                if (event !is SpellCastEvent) return false
                if (!matchesPlayer(trigger.player, event.casterId, controllerId)) return false
                val currentCount = state.playerSpellsCastThisTurn[event.casterId] ?: 0
                currentCount == trigger.nthSpell
            }
            is GameEvent.ExpendEvent -> {
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
            is GameEvent.SpellOrAbilityOnStackEvent -> {
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
            is GameEvent.CycleEvent -> {
                event is CardCycledEvent &&
                    matchesPlayer(trigger.player, event.playerId, controllerId) &&
                    (binding != TriggerBinding.SELF || event.cardId == sourceId)
            }
            is GameEvent.GiftGivenEvent -> {
                event is GiftGivenEvent &&
                    matchesPlayer(trigger.player, event.controllerId, controllerId)
            }
            is GameEvent.TapEvent -> {
                event is TappedEvent && (binding != TriggerBinding.SELF || event.entityId == sourceId)
            }
            is GameEvent.UntapEvent -> {
                event is UntappedEvent && (binding != TriggerBinding.SELF || event.entityId == sourceId)
            }
            is GameEvent.LifeGainEvent -> {
                event is LifeChangedEvent &&
                    event.reason == com.wingedsheep.engine.core.LifeChangeReason.LIFE_GAIN &&
                    matchesPlayer(trigger.player, event.playerId, controllerId)
            }
            is GameEvent.BecomesTargetEvent -> {
                event is BecomesTargetEvent && matchesBecomesTargetTrigger(trigger, binding, event, sourceId, controllerId, state)
            }
            is GameEvent.TurnFaceUpEvent -> {
                event is TurnFaceUpEvent && (binding != TriggerBinding.SELF || event.entityId == sourceId)
            }
            is GameEvent.CreatureTurnedFaceUpEvent -> {
                if (event !is TurnFaceUpEvent) return false
                matchesPlayer(trigger.player, event.controllerId, controllerId)
            }
            is GameEvent.TransformEvent -> {
                // Transform not yet implemented in new engine
                false
            }
            // These are handled separately in their own detect* methods
            is GameEvent.ControlChangeEvent -> false
            // Phase/step triggers are handled separately
            is GameEvent.StepEvent -> false
            // Creature-dealt-damage-by-source-dies triggers are handled separately
            is GameEvent.CreatureDealtDamageBySourceDiesEvent -> false
            // Replacement-effect-only events never match as triggers
            is GameEvent.DamageEvent -> false
            is GameEvent.CounterPlacementEvent -> false
            is GameEvent.TokenCreationEvent -> false
            is GameEvent.LifeLossEvent -> {
                event is LifeChangedEvent &&
                    event.reason != com.wingedsheep.engine.core.LifeChangeReason.LIFE_GAIN &&
                    event.oldLife > event.newLife &&
                    matchesPlayer(trigger.player, event.playerId, controllerId)
            }
            is GameEvent.LifeGainOrLossEvent -> {
                event is LifeChangedEvent &&
                    event.oldLife != event.newLife &&
                    matchesPlayer(trigger.player, event.playerId, controllerId)
            }
            is GameEvent.DiscardEvent -> false
            is GameEvent.SearchLibraryEvent -> false
            // ExtraTurnEvent is only used as a replacement effect filter, not a trigger
            is GameEvent.ExtraTurnEvent -> false
            // Batching trigger — handled in detectLibraryToGraveyardBatchTriggers
            is GameEvent.CardsPutIntoGraveyardFromLibraryEvent -> false
            // Sacrifice batch triggers are handled by detectSacrificeBatchTriggers
            is GameEvent.PermanentsSacrificedEvent -> false
            // Combat damage batch triggers are handled by detectCombatDamageBatchTriggers
            is GameEvent.OneOrMoreDealCombatDamageToPlayerEvent -> false
            // Leave battlefield without dying batch triggers are handled by detectLeaveBattlefieldWithoutDyingBatchTriggers
            is GameEvent.LeaveBattlefieldWithoutDyingEvent -> false
            // Enter battlefield batch triggers are handled by detectPermanentsEnteredBatchTriggers
            is GameEvent.PermanentsEnteredEvent -> false
            is GameEvent.CountersPlacedEvent -> {
                if (event !is CountersAddedEvent) return false
                if (!counterTypesMatch(trigger.counterType, event.counterType)) return false
                // Check filter: the permanent receiving counters must match
                if (trigger.filter != GameObjectFilter.Any) {
                    val projected = state.projectedState
                    val predicateContext = com.wingedsheep.engine.handlers.PredicateContext(
                        controllerId = controllerId,
                        sourceId = sourceId
                    )
                    val predicateEvaluator = PredicateEvaluator()
                    if (!predicateEvaluator.matchesWithProjection(state, projected, event.entityId, trigger.filter, predicateContext)) {
                        return false
                    }
                }
                true
            }
        }
    }

    fun matchesZoneChangeTrigger(
        trigger: GameEvent.ZoneChangeEvent,
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
                        // Face-down permanents are 2/2 creatures (Rule 707.2) and count.
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
                    else -> {
                        // For other predicates, check the entity's type
                        if (cardComponent == null) return false
                        if (!matchesCardPredicate(
                                predicate, cardComponent, projected, event.entityId, isFaceDown,
                                lastKnownPower = event.lastKnownPower,
                                lastKnownToughness = event.lastKnownToughness
                            )) return false
                    }
                }
            }
            // Check state predicates (face-down, tapped, etc.)
            for (predicate in trigger.filter.statePredicates) {
                if (!matchesStatePredicateForTrigger(predicate, state, event.entityId)) return false
            }
            // Check controller predicate (youControl)
            if (trigger.filter.controllerPredicate != null) {
                when (trigger.filter.controllerPredicate) {
                    is com.wingedsheep.sdk.scripting.predicates.ControllerPredicate.ControlledByYou -> {
                        if (event.ownerId != controllerId) return false
                    }
                    is com.wingedsheep.sdk.scripting.predicates.ControllerPredicate.ControlledByOpponent -> {
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
        lastKnownToughness: Int? = null
    ): Boolean {
        return when (predicate) {
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature -> cardComponent.typeLine.isCreature
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsLand -> cardComponent.typeLine.isLand
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsArtifact -> cardComponent.typeLine.isArtifact
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsEnchantment -> cardComponent.typeLine.isEnchantment
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsPlaneswalker -> com.wingedsheep.sdk.core.CardType.PLANESWALKER in cardComponent.typeLine.cardTypes
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsInstant -> cardComponent.typeLine.isInstant
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsSorcery -> cardComponent.typeLine.isSorcery
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsBasicLand -> cardComponent.typeLine.isLand && cardComponent.typeLine.supertypes.contains(com.wingedsheep.sdk.core.Supertype.BASIC)
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
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueEquals -> {
                val cmc = if (isFaceDown) 0 else cardComponent.manaValue
                cmc == predicate.value
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
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasKeyword ->
                projected.hasKeyword(entityId, predicate.keyword)
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.NotKeyword ->
                !projected.hasKeyword(entityId, predicate.keyword)
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.Or ->
                predicate.predicates.any { matchesCardPredicate(it, cardComponent, projected, entityId, isFaceDown, lastKnownPower, lastKnownToughness) }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.And ->
                predicate.predicates.all { matchesCardPredicate(it, cardComponent, projected, entityId, isFaceDown, lastKnownPower, lastKnownToughness) }
            is com.wingedsheep.sdk.scripting.predicates.CardPredicate.Not ->
                !matchesCardPredicate(predicate.predicate, cardComponent, projected, entityId, isFaceDown, lastKnownPower, lastKnownToughness)
            else -> true // Unknown predicates pass through
        }
    }

    /**
     * Check if a trigger event matches a death trigger pattern (ZoneChangeEvent from battlefield to graveyard).
     */
    fun isDeathTrigger(trigger: GameEvent): Boolean {
        return trigger is GameEvent.ZoneChangeEvent &&
            trigger.from == Zone.BATTLEFIELD &&
            trigger.to == Zone.GRAVEYARD
    }

    /**
     * Check if a trigger event matches a leaves-battlefield pattern (ZoneChangeEvent from battlefield, to=null).
     */
    fun isLeavesBattlefieldTrigger(trigger: GameEvent): Boolean {
        return trigger is GameEvent.ZoneChangeEvent &&
            trigger.from == Zone.BATTLEFIELD &&
            trigger.to == null
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
        trigger: GameEvent.BecomesTargetEvent,
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
        trigger: GameEvent.DealsDamageEvent,
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
            RecipientFilter.AnyCreature -> event.targetId !in state.turnOrder
            RecipientFilter.You -> false // handled separately in detectDamageToControllerTriggers
            RecipientFilter.Opponent -> {
                event.targetId in state.turnOrder && event.targetId != controllerId
            }
            RecipientFilter.CreatureOpponentControls -> {
                val targetEntity = state.getEntity(event.targetId)
                val targetCard = targetEntity?.get<CardComponent>()
                val targetController = targetEntity?.get<ControllerComponent>()?.playerId
                targetCard?.typeLine?.isCreature == true && targetController != null && targetController != controllerId
            }
            RecipientFilter.CreatureYouControl -> {
                val targetEntity = state.getEntity(event.targetId)
                val targetCard = targetEntity?.get<CardComponent>()
                val targetController = targetEntity?.get<ControllerComponent>()?.playerId
                targetCard?.typeLine?.isCreature == true && targetController == controllerId
            }
            RecipientFilter.PermanentYouControl -> {
                val targetController = state.getEntity(event.targetId)?.get<ControllerComponent>()?.playerId
                targetController == controllerId
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
            predicateEvaluator.matchesWithProjection(
                state, state.projectedState, event.sourceId, trigger.sourceFilter!!, predicateContext
            )
        } else {
            true
        }
        return combatMatches && recipientMatches && sourceMatches
    }

    fun matchesStepTrigger(
        trigger: GameEvent,
        step: Step,
        controllerId: EntityId,
        activePlayerId: EntityId
    ): Boolean {
        if (trigger !is GameEvent.StepEvent) return false
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
        state: GameState
    ): Boolean {
        // No card predicates = match any spell (equivalent to old SpellTypeFilter.ANY)
        if (spellFilter.cardPredicates.isEmpty()) return true

        val container = state.getEntity(event.spellEntityId) ?: return false

        // Face-down spells have no characteristics (CR 707.2) — they don't match any type filter
        val isFaceDown = container.get<SpellOnStackComponent>()?.castFaceDown == true
        if (isFaceDown) return false

        // Use base-state matching (spells on the stack don't get continuous effects)
        val context = com.wingedsheep.engine.handlers.PredicateContext(controllerId = event.casterId)
        return predicateEvaluator.matches(state, event.spellEntityId, spellFilter, context)
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
                triggeringEntityId = trigger.triggerContext.triggeringEntityId
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
        else -> true
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

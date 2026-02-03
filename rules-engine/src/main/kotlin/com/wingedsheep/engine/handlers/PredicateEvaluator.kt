package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CardPredicate
import com.wingedsheep.sdk.scripting.ControllerPredicate
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.StatePredicate

/**
 * Evaluates the new unified predicates and filters against game state.
 *
 * This provides evaluation for:
 * - CardPredicate (type, color, subtype, mana value, P/T, keywords)
 * - StatePredicate (tapped, attacking, blocking)
 * - ControllerPredicate (youControl, opponentControls)
 * - GameObjectFilter (composed from the above predicates)
 */
class PredicateEvaluator {

    /**
     * Evaluate a GameObjectFilter against an entity.
     *
     * @param state Current game state
     * @param entityId Entity to evaluate
     * @param filter The filter to match against
     * @param context Evaluation context (for controller resolution)
     * @return true if the entity matches all predicates in the filter
     */
    fun matches(
        state: GameState,
        entityId: EntityId,
        filter: GameObjectFilter,
        context: PredicateContext
    ): Boolean {
        val container = state.getEntity(entityId) ?: return false

        // Check controller predicate first (if specified)
        filter.controllerPredicate?.let { controllerPred ->
            if (!matchesControllerPredicate(state, entityId, controllerPred, context)) {
                return false
            }
        }

        // Check all card predicates
        val cardMatches = if (filter.matchAll) {
            filter.cardPredicates.all { predicate ->
                matchesCardPredicate(state, entityId, predicate)
            }
        } else {
            filter.cardPredicates.isEmpty() || filter.cardPredicates.any { predicate ->
                matchesCardPredicate(state, entityId, predicate)
            }
        }

        if (!cardMatches) return false

        // Check all state predicates
        val stateMatches = filter.statePredicates.all { predicate ->
            matchesStatePredicate(state, entityId, predicate)
        }

        return stateMatches
    }

    /**
     * Evaluate a CardPredicate against an entity.
     */
    fun matchesCardPredicate(
        state: GameState,
        entityId: EntityId,
        predicate: CardPredicate
    ): Boolean {
        val container = state.getEntity(entityId) ?: return false
        val card = container.get<CardComponent>() ?: return false
        val typeLine = card.typeLine

        return when (predicate) {
            // Type predicates
            CardPredicate.IsCreature -> typeLine.isCreature
            CardPredicate.IsLand -> typeLine.isLand
            CardPredicate.IsArtifact -> typeLine.isArtifact
            CardPredicate.IsEnchantment -> typeLine.isEnchantment
            CardPredicate.IsPlaneswalker -> card.isPlaneswalker
            CardPredicate.IsInstant -> typeLine.isInstant
            CardPredicate.IsSorcery -> typeLine.isSorcery
            CardPredicate.IsBasicLand -> typeLine.isBasicLand
            CardPredicate.IsPermanent -> typeLine.isPermanent
            CardPredicate.IsNonland -> !typeLine.isLand
            CardPredicate.IsNoncreature -> !typeLine.isCreature
            CardPredicate.IsToken -> container.has<TokenComponent>()
            CardPredicate.IsNontoken -> !container.has<TokenComponent>()

            // Color predicates
            is CardPredicate.HasColor -> predicate.color in card.colors
            is CardPredicate.NotColor -> predicate.color !in card.colors
            CardPredicate.IsColorless -> card.colors.isEmpty()
            CardPredicate.IsMulticolored -> card.colors.size > 1
            CardPredicate.IsMonocolored -> card.colors.size == 1

            // Subtype predicates
            is CardPredicate.HasSubtype -> typeLine.hasSubtype(predicate.subtype)
            is CardPredicate.NotSubtype -> !typeLine.hasSubtype(predicate.subtype)
            is CardPredicate.HasBasicLandType -> {
                typeLine.hasSubtype(com.wingedsheep.sdk.core.Subtype(predicate.landType))
            }

            // Keyword predicates
            is CardPredicate.HasKeyword -> {
                // Keywords are stored in the card's baseKeywords
                card.baseKeywords.contains(predicate.keyword)
            }
            is CardPredicate.NotKeyword -> {
                !card.baseKeywords.contains(predicate.keyword)
            }

            // Mana value predicates
            is CardPredicate.ManaValueEquals -> card.manaValue == predicate.value
            is CardPredicate.ManaValueAtMost -> card.manaValue <= predicate.max
            is CardPredicate.ManaValueAtLeast -> card.manaValue >= predicate.min

            // Power/toughness predicates
            is CardPredicate.PowerEquals -> {
                val power = card.baseStats?.power
                power is com.wingedsheep.sdk.model.CharacteristicValue.Fixed && power.value == predicate.value
            }
            is CardPredicate.PowerAtMost -> {
                val power = card.baseStats?.power
                power is com.wingedsheep.sdk.model.CharacteristicValue.Fixed && power.value <= predicate.max
            }
            is CardPredicate.PowerAtLeast -> {
                val power = card.baseStats?.power
                power is com.wingedsheep.sdk.model.CharacteristicValue.Fixed && power.value >= predicate.min
            }
            is CardPredicate.ToughnessEquals -> {
                val toughness = card.baseStats?.toughness
                toughness is com.wingedsheep.sdk.model.CharacteristicValue.Fixed && toughness.value == predicate.value
            }
            is CardPredicate.ToughnessAtMost -> {
                val toughness = card.baseStats?.toughness
                toughness is com.wingedsheep.sdk.model.CharacteristicValue.Fixed && toughness.value <= predicate.max
            }
            is CardPredicate.ToughnessAtLeast -> {
                val toughness = card.baseStats?.toughness
                toughness is com.wingedsheep.sdk.model.CharacteristicValue.Fixed && toughness.value >= predicate.min
            }

            // Composite predicates
            is CardPredicate.And -> {
                predicate.predicates.all { matchesCardPredicate(state, entityId, it) }
            }
            is CardPredicate.Or -> {
                predicate.predicates.any { matchesCardPredicate(state, entityId, it) }
            }
            is CardPredicate.Not -> {
                !matchesCardPredicate(state, entityId, predicate.predicate)
            }
        }
    }

    /**
     * Evaluate a StatePredicate against an entity.
     */
    fun matchesStatePredicate(
        state: GameState,
        entityId: EntityId,
        predicate: StatePredicate
    ): Boolean {
        val container = state.getEntity(entityId) ?: return false

        return when (predicate) {
            // Tap state
            StatePredicate.IsTapped -> container.has<TappedComponent>()
            StatePredicate.IsUntapped -> !container.has<TappedComponent>()

            // Combat state
            StatePredicate.IsAttacking -> container.has<AttackingComponent>()
            StatePredicate.IsBlocking -> container.has<BlockingComponent>()
            StatePredicate.IsAttackingOrBlocking -> {
                container.has<AttackingComponent>() || container.has<BlockingComponent>()
            }
            StatePredicate.IsBlocked -> {
                // Check if this attacking creature has any blockers assigned
                val attackingComp = container.get<AttackingComponent>()
                attackingComp != null && state.getBattlefield().any { blockerId ->
                    state.getEntity(blockerId)?.get<BlockingComponent>()?.blockedAttackerIds?.contains(entityId) == true
                }
            }
            StatePredicate.IsUnblocked -> {
                val attackingComp = container.get<AttackingComponent>()
                attackingComp != null && state.getBattlefield().none { blockerId ->
                    state.getEntity(blockerId)?.get<BlockingComponent>()?.blockedAttackerIds?.contains(entityId) == true
                }
            }

            // Summoning sickness / ETB
            StatePredicate.EnteredThisTurn -> {
                // Check if the entity entered the battlefield this turn
                // This requires turn tracking - for now return false
                // TODO: Implement with ETB tracking
                false
            }

            // Damage state
            StatePredicate.WasDealtDamageThisTurn -> {
                // TODO: Implement damage tracking
                false
            }
            StatePredicate.HasDealtDamage -> {
                // TODO: Implement damage tracking
                false
            }
            StatePredicate.HasDealtCombatDamageToPlayer -> {
                // TODO: Implement damage tracking
                false
            }
        }
    }

    /**
     * Evaluate a ControllerPredicate against an entity.
     */
    fun matchesControllerPredicate(
        state: GameState,
        entityId: EntityId,
        predicate: ControllerPredicate,
        context: PredicateContext
    ): Boolean {
        val container = state.getEntity(entityId) ?: return false
        val controllerId = container.get<ControllerComponent>()?.playerId ?: return false

        return when (predicate) {
            ControllerPredicate.ControlledByYou -> controllerId == context.controllerId
            ControllerPredicate.ControlledByOpponent -> controllerId != context.controllerId
            ControllerPredicate.ControlledByAny -> true
            ControllerPredicate.ControlledByTargetOpponent -> {
                context.targetOpponentId?.let { controllerId == it } ?: false
            }
            ControllerPredicate.ControlledByTargetPlayer -> {
                context.targetPlayerId?.let { controllerId == it } ?: false
            }
            ControllerPredicate.OwnedByYou -> {
                // Owner is different from controller - check the card's owner
                val card = container.get<CardComponent>()
                card?.ownerId == context.controllerId
            }
            ControllerPredicate.OwnedByOpponent -> {
                val card = container.get<CardComponent>()
                card?.ownerId != null && card.ownerId != context.controllerId
            }
        }
    }
}

/**
 * Context for predicate evaluation, providing player references.
 */
data class PredicateContext(
    val controllerId: EntityId,
    val targetOpponentId: EntityId? = null,
    val targetPlayerId: EntityId? = null,
    val sourceId: EntityId? = null,
    /** Owner of the entity being evaluated (for graveyard targeting) */
    val ownerId: EntityId? = null
) {
    companion object {
        /**
         * Create from EffectContext for compatibility.
         */
        fun fromEffectContext(context: EffectContext): PredicateContext {
            return PredicateContext(
                controllerId = context.controllerId,
                targetOpponentId = context.opponentId,
                targetPlayerId = context.opponentId,
                sourceId = context.sourceId
            )
        }
    }
}

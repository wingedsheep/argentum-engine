package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CastFromHandComponent
import com.wingedsheep.engine.state.components.battlefield.WasKickedComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.combat.PlayerAttackedThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.conditions.APlayerControlsMostOfSubtype
import com.wingedsheep.sdk.scripting.conditions.YouControlMostOfChosenType
import com.wingedsheep.sdk.scripting.conditions.AllConditions
import com.wingedsheep.sdk.scripting.conditions.AnyCondition
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.conditions.EnchantedCreatureHasSubtype
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.conditions.IsNotYourTurn
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn
import com.wingedsheep.sdk.scripting.conditions.NotCondition
import com.wingedsheep.sdk.scripting.conditions.OpponentSpellOnStack
import com.wingedsheep.sdk.scripting.conditions.PlayedLandThisTurn
import com.wingedsheep.sdk.scripting.conditions.SourceEnteredThisTurn
import com.wingedsheep.sdk.scripting.conditions.SourceHasDealtCombatDamageToPlayer
import com.wingedsheep.sdk.scripting.conditions.SourceHasDealtDamage
import com.wingedsheep.sdk.scripting.conditions.SourceHasKeyword
import com.wingedsheep.sdk.scripting.conditions.SourceHasSubtype
import com.wingedsheep.sdk.scripting.conditions.SourceIsAttacking
import com.wingedsheep.sdk.scripting.conditions.SourceIsBlocking
import com.wingedsheep.sdk.scripting.conditions.SourceIsTapped
import com.wingedsheep.sdk.scripting.conditions.SourceIsUntapped
import com.wingedsheep.sdk.scripting.conditions.WasCastFromHand
import com.wingedsheep.sdk.scripting.conditions.WasCastFromZone
import com.wingedsheep.sdk.scripting.conditions.SacrificedPermanentHadSubtype
import com.wingedsheep.sdk.scripting.conditions.TargetMatchesFilter
import com.wingedsheep.sdk.scripting.conditions.TriggeringEntityWasHistoric
import com.wingedsheep.sdk.scripting.conditions.CardsLeftGraveyardThisTurn
import com.wingedsheep.sdk.scripting.conditions.OpponentLostLifeThisTurn
import com.wingedsheep.sdk.scripting.conditions.SacrificedFoodThisTurn
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.conditions.YouAttackedThisTurn
import com.wingedsheep.sdk.scripting.conditions.YouWereAttackedThisStep
import com.wingedsheep.sdk.scripting.conditions.YouWereDealtCombatDamageThisTurn

/**
 * Evaluates conditions from the SDK against the game state.
 */
class ConditionEvaluator {

    private val dynamicAmountEvaluator = DynamicAmountEvaluator(this)

    /**
     * Evaluate a condition and return true if met.
     */
    fun evaluate(
        state: GameState,
        condition: Condition,
        context: EffectContext
    ): Boolean {
        return when (condition) {
            // Generic primitives
            is Compare -> evaluateCompare(state, condition, context)
            is Exists -> evaluateExists(state, condition, context)

            // Battlefield conditions (non-generic)
            is APlayerControlsMostOfSubtype -> evaluateAPlayerControlsMostOfSubtype(state, condition)
            is YouControlMostOfChosenType -> evaluateYouControlMostOfChosenType(state, condition, context)
            is EnchantedCreatureHasSubtype -> evaluateEnchantedCreatureHasSubtype(state, condition, context)

            // Source conditions
            is WasCastFromHand -> evaluateWasCastFromHand(state, context)
            is WasCastFromZone -> evaluateWasCastFromZone(state, condition, context)
            is WasKicked -> evaluateWasKicked(state, context)
            is SourceIsAttacking -> evaluateSourceAttacking(state, context)
            is SourceIsBlocking -> evaluateSourceBlocking(state, context)
            is SourceIsTapped -> evaluateSourceTapped(state, context)
            is SourceIsUntapped -> evaluateSourceUntapped(state, context)
            is SourceEnteredThisTurn -> evaluateSourceEnteredThisTurn(state, context)
            is SourceHasDealtDamage -> evaluateSourceHasDealtDamage(state, context)
            is SourceHasDealtCombatDamageToPlayer -> evaluateSourceHasDealtCombatDamageToPlayer(state, context)
            is SourceHasSubtype -> evaluateSourceHasSubtype(state, condition, context)
            is SourceHasKeyword -> evaluateSourceHasKeyword(state, condition, context)
            is SacrificedPermanentHadSubtype -> evaluateSacrificedPermanentHadSubtype(condition, context)
            is TriggeringEntityWasHistoric -> evaluateTriggeringEntityWasHistoric(state, context)
            is TargetMatchesFilter -> evaluateTargetMatchesFilter(state, condition, context)

            // Turn conditions
            is IsYourTurn -> evaluateIsYourTurn(state, context)
            is IsNotYourTurn -> !evaluateIsYourTurn(state, context)
            is PlayedLandThisTurn -> evaluatePlayedLandThisTurn(state, context)
            is YouAttackedThisTurn -> evaluateYouAttackedThisTurn(state, context)
            is OpponentLostLifeThisTurn -> evaluateOpponentLostLifeThisTurn(state, context)
            is YouWereAttackedThisStep -> evaluateYouWereAttackedThisStep(state, context)
            is YouWereDealtCombatDamageThisTurn -> evaluateYouWereDealtCombatDamageThisTurn(state, context)
            is CardsLeftGraveyardThisTurn -> evaluateCardsLeftGraveyardThisTurn(state, condition, context)
            is SacrificedFoodThisTurn -> evaluateSacrificedFoodThisTurn(state, context)

            // Stack conditions
            is OpponentSpellOnStack -> evaluateOpponentSpellOnStack(state, context)

            // Composite conditions
            is AllConditions -> condition.conditions.all { evaluate(state, it, context) }
            is AnyCondition -> condition.conditions.any { evaluate(state, it, context) }
            is NotCondition -> !evaluate(state, condition.condition, context)
        }
    }

    private fun evaluateCompare(state: GameState, condition: Compare, context: EffectContext): Boolean {
        val left = dynamicAmountEvaluator.evaluate(state, condition.left, context)
        val right = dynamicAmountEvaluator.evaluate(state, condition.right, context)
        return when (condition.operator) {
            ComparisonOperator.LT -> left < right
            ComparisonOperator.LTE -> left <= right
            ComparisonOperator.EQ -> left == right
            ComparisonOperator.NEQ -> left != right
            ComparisonOperator.GT -> left > right
            ComparisonOperator.GTE -> left >= right
        }
    }

    private fun evaluateExists(state: GameState, condition: Exists, context: EffectContext): Boolean {
        val predicateEvaluator = PredicateEvaluator()
        val predicateContext = PredicateContext.fromEffectContext(context)
        val projected = state.projectedState

        val playerIds = when (condition.player) {
            is com.wingedsheep.sdk.scripting.references.Player.You -> listOf(context.controllerId)
            is com.wingedsheep.sdk.scripting.references.Player.Opponent -> state.turnOrder.filter { it != context.controllerId }
            is com.wingedsheep.sdk.scripting.references.Player.EachOpponent -> state.turnOrder.filter { it != context.controllerId }
            is com.wingedsheep.sdk.scripting.references.Player.Each -> state.turnOrder
            else -> listOf(context.controllerId)
        }

        val found = playerIds.any { playerId ->
            val entities = if (condition.zone == Zone.BATTLEFIELD) {
                state.getBattlefield().filter { entityId ->
                    state.getEntity(entityId)?.get<ControllerComponent>()?.playerId == playerId
                }
            } else {
                state.getZone(ZoneKey(playerId, condition.zone))
            }

            if (condition.filter == GameObjectFilter.Any) {
                entities.isNotEmpty()
            } else {
                entities.any { entityId ->
                    predicateEvaluator.matchesWithProjection(state, projected, entityId, condition.filter, predicateContext)
                }
            }
        }

        return if (condition.negate) !found else found
    }

    private fun evaluateWasCastFromHand(state: GameState, context: EffectContext): Boolean {
        val sourceId = context.sourceId ?: return false
        return state.getEntity(sourceId)?.has<CastFromHandComponent>() == true
    }

    private fun evaluateWasCastFromZone(state: GameState, condition: WasCastFromZone, context: EffectContext): Boolean {
        // For spells resolving, check context.castFromZone (set from SpellOnStackComponent)
        if (context.castFromZone == condition.zone) return true
        // For permanents (triggered abilities), fall back to battlefield components
        val sourceId = context.sourceId ?: return false
        if (condition.zone == Zone.HAND) {
            return state.getEntity(sourceId)?.has<CastFromHandComponent>() == true
        }
        return false
    }

    private fun evaluateWasKicked(state: GameState, context: EffectContext): Boolean {
        // Check the component on the permanent first (for triggered abilities)
        val sourceId = context.sourceId ?: return context.wasKicked
        if (state.getEntity(sourceId)?.has<WasKickedComponent>() == true) return true
        // Fall back to context (for spell resolution, e.g. kicker additional effects)
        return context.wasKicked
    }

    private fun evaluateSourceAttacking(state: GameState, context: EffectContext): Boolean {
        val sourceId = context.sourceId ?: return false
        return state.getEntity(sourceId)?.has<AttackingComponent>() == true
    }

    private fun evaluateSourceBlocking(state: GameState, context: EffectContext): Boolean {
        val sourceId = context.sourceId ?: return false
        return state.getEntity(sourceId)?.has<BlockingComponent>() == true
    }

    private fun evaluateSourceTapped(state: GameState, context: EffectContext): Boolean {
        val sourceId = context.sourceId ?: return false
        return state.getEntity(sourceId)?.has<TappedComponent>() == true
    }

    private fun evaluateSourceUntapped(state: GameState, context: EffectContext): Boolean {
        val sourceId = context.sourceId ?: return false
        return state.getEntity(sourceId)?.has<TappedComponent>() != true
    }

    private fun evaluateIsYourTurn(state: GameState, context: EffectContext): Boolean {
        return state.activePlayerId == context.controllerId
    }

    private fun evaluatePlayedLandThisTurn(state: GameState, context: EffectContext): Boolean {
        val landDrops = state.getEntity(context.controllerId)?.get<LandDropsComponent>()
            ?: return false
        return landDrops.remaining < landDrops.maxPerTurn
    }

    private fun evaluateSourceEnteredThisTurn(state: GameState, context: EffectContext): Boolean {
        // TODO: Track when permanents entered the battlefield
        return false
    }

    private fun evaluateSourceHasDealtDamage(state: GameState, context: EffectContext): Boolean {
        // TODO: Track damage dealt by source this turn
        return false
    }

    private fun evaluateSourceHasSubtype(state: GameState, condition: SourceHasSubtype, context: EffectContext): Boolean {
        val sourceId = context.sourceId ?: return false
        val card = state.getEntity(sourceId)?.get<CardComponent>() ?: return false
        return card.typeLine.hasSubtype(condition.subtype) ||
            // Changeling: has all creature types in all zones
            (Keyword.CHANGELING in card.baseKeywords && condition.subtype.value in Subtype.ALL_CREATURE_TYPES)
    }

    private fun evaluateSourceHasKeyword(state: GameState, condition: SourceHasKeyword, context: EffectContext): Boolean {
        val sourceId = context.sourceId ?: return false
        val projected = state.projectedState
        return projected.hasKeyword(sourceId, condition.keyword)
    }

    private fun evaluateSacrificedPermanentHadSubtype(
        condition: SacrificedPermanentHadSubtype,
        context: EffectContext
    ): Boolean {
        return context.sacrificedPermanentSubtypes.values.any { subtypes ->
            subtypes.contains(condition.subtype)
        }
    }

    private fun evaluateSourceHasDealtCombatDamageToPlayer(state: GameState, context: EffectContext): Boolean {
        // TODO: Track combat damage dealt to players
        return false
    }

    private fun evaluateYouAttackedThisTurn(state: GameState, context: EffectContext): Boolean {
        return state.getEntity(context.controllerId)?.has<PlayerAttackedThisTurnComponent>() == true
    }

    private fun evaluateOpponentLostLifeThisTurn(state: GameState, context: EffectContext): Boolean {
        val opponents = state.turnOrder.filter { it != context.controllerId }
        return opponents.any { opponentId ->
            state.getEntity(opponentId)?.has<com.wingedsheep.engine.state.components.player.LifeLostThisTurnComponent>() == true
        }
    }

    private fun evaluateYouWereAttackedThisStep(state: GameState, context: EffectContext): Boolean {
        return state.entities.any { (_, container) ->
            val attacking = container.get<AttackingComponent>()
            attacking != null && attacking.defenderId == context.controllerId
        }
    }

    private fun evaluateYouWereDealtCombatDamageThisTurn(state: GameState, context: EffectContext): Boolean {
        // TODO: Track if player was dealt combat damage this turn
        return false
    }

    private fun evaluateCardsLeftGraveyardThisTurn(state: GameState, condition: CardsLeftGraveyardThisTurn, context: EffectContext): Boolean {
        val component = state.getEntity(context.controllerId)
            ?.get<com.wingedsheep.engine.state.components.player.CardsLeftGraveyardThisTurnComponent>()
        return (component?.count ?: 0) >= condition.count
    }

    private fun evaluateSacrificedFoodThisTurn(state: GameState, context: EffectContext): Boolean {
        return state.getEntity(context.controllerId)
            ?.has<com.wingedsheep.engine.state.components.player.SacrificedFoodThisTurnComponent>() == true
    }

    private fun evaluateOpponentSpellOnStack(state: GameState, context: EffectContext): Boolean {
        val opponentId = context.opponentId ?: return false
        return state.stack.any { entityId ->
            val container = state.getEntity(entityId) ?: return@any false
            container.get<com.wingedsheep.engine.state.components.stack.SpellOnStackComponent>()?.casterId == opponentId
        }
    }

    private fun evaluateAPlayerControlsMostOfSubtype(
        state: GameState,
        condition: APlayerControlsMostOfSubtype
    ): Boolean {
        val counts = state.turnOrder.associateWith { playerId ->
            state.entities.count { (_, container) ->
                container.get<ControllerComponent>()?.playerId == playerId &&
                container.get<CardComponent>()?.typeLine?.hasSubtype(condition.subtype) == true
            }
        }
        val maxCount = counts.values.maxOrNull() ?: return false
        if (maxCount == 0) return false
        val playersWithMax = counts.count { it.value == maxCount }
        return playersWithMax == 1
    }

    private fun evaluateYouControlMostOfChosenType(
        state: GameState,
        condition: YouControlMostOfChosenType,
        context: EffectContext
    ): Boolean {
        val chosenType = context.pipeline.chosenValues[condition.chosenValueKey] ?: return false
        val projected = state.projectedState
        val controllerId = context.controllerId

        val counts = state.turnOrder.associateWith { playerId ->
            state.getBattlefield().count { entityId ->
                val controller = projected.getController(entityId)
                controller == playerId && projected.hasSubtype(entityId, chosenType) &&
                    projected.isCreature(entityId)
            }
        }

        val controllerCount = counts[controllerId] ?: 0
        if (controllerCount == 0) return false

        return counts.filter { it.key != controllerId }.all { controllerCount > it.value }
    }

    private fun evaluateEnchantedCreatureHasSubtype(
        state: GameState,
        condition: EnchantedCreatureHasSubtype,
        context: EffectContext
    ): Boolean {
        val sourceId = context.sourceId ?: return false
        // If sourceId has AttachedToComponent, it's the aura — check the attached creature.
        // Otherwise, the source IS the enchanted creature (ability was granted via GrantActivatedAbilityToAttachedCreature).
        val creatureId = state.getEntity(sourceId)?.get<AttachedToComponent>()?.targetId ?: sourceId
        val projected = state.projectedState
        return projected.hasSubtype(creatureId, condition.subtype.value)
    }

    private fun evaluateTriggeringEntityWasHistoric(state: GameState, context: EffectContext): Boolean {
        val entityId = context.triggeringEntityId ?: return false
        val card = state.getEntity(entityId)?.get<CardComponent>() ?: return false
        return card.typeLine.isHistoric
    }

    private fun evaluateTargetMatchesFilter(
        state: GameState,
        condition: TargetMatchesFilter,
        context: EffectContext
    ): Boolean {
        val target = context.targets.getOrNull(condition.targetIndex) ?: return false
        val entityId = when (target) {
            is com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent -> target.entityId
            is com.wingedsheep.engine.state.components.stack.ChosenTarget.Player -> return false
            is com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell -> target.spellEntityId
            is com.wingedsheep.engine.state.components.stack.ChosenTarget.Card -> target.cardId
        }
        val predicateEvaluator = PredicateEvaluator()
        val predicateContext = PredicateContext.fromEffectContext(context)
        val projected = state.projectedState
        return predicateEvaluator.matchesWithProjection(state, projected, entityId, condition.filter, predicateContext)
    }
}

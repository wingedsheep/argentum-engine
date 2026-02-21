package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.conditions.APlayerControlsMostOfSubtype
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
import com.wingedsheep.sdk.scripting.conditions.SourceHasSubtype
import com.wingedsheep.sdk.scripting.conditions.SourceIsAttacking
import com.wingedsheep.sdk.scripting.conditions.SourceIsBlocking
import com.wingedsheep.sdk.scripting.conditions.SourceIsTapped
import com.wingedsheep.sdk.scripting.conditions.SourceIsUntapped
import com.wingedsheep.sdk.scripting.conditions.TargetPowerAtMost
import com.wingedsheep.sdk.scripting.conditions.TargetSpellManaValueAtMost
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
            is TargetPowerAtMost -> evaluateTargetPowerAtMost(state, condition, context)
            is TargetSpellManaValueAtMost -> evaluateTargetSpellManaValueAtMost(state, condition, context)
            is EnchantedCreatureHasSubtype -> evaluateEnchantedCreatureHasSubtype(state, condition, context)

            // Source conditions
            is SourceIsAttacking -> evaluateSourceAttacking(state, context)
            is SourceIsBlocking -> evaluateSourceBlocking(state, context)
            is SourceIsTapped -> evaluateSourceTapped(state, context)
            is SourceIsUntapped -> evaluateSourceUntapped(state, context)
            is SourceEnteredThisTurn -> evaluateSourceEnteredThisTurn(state, context)
            is SourceHasDealtDamage -> evaluateSourceHasDealtDamage(state, context)
            is SourceHasDealtCombatDamageToPlayer -> evaluateSourceHasDealtCombatDamageToPlayer(state, context)
            is SourceHasSubtype -> evaluateSourceHasSubtype(state, condition, context)

            // Turn conditions
            is IsYourTurn -> evaluateIsYourTurn(state, context)
            is IsNotYourTurn -> !evaluateIsYourTurn(state, context)
            is PlayedLandThisTurn -> evaluatePlayedLandThisTurn(state, context)
            is YouAttackedThisTurn -> evaluateYouAttackedThisTurn(state, context)
            is YouWereAttackedThisStep -> evaluateYouWereAttackedThisStep(state, context)
            is YouWereDealtCombatDamageThisTurn -> evaluateYouWereDealtCombatDamageThisTurn(state, context)

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
                    predicateEvaluator.matches(state, entityId, condition.filter, predicateContext)
                }
            }
        }

        return if (condition.negate) !found else found
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
        return state.getEntity(sourceId)?.get<CardComponent>()?.typeLine?.hasSubtype(condition.subtype) == true
    }

    private fun evaluateSourceHasDealtCombatDamageToPlayer(state: GameState, context: EffectContext): Boolean {
        // TODO: Track combat damage dealt to players
        return false
    }

    private fun evaluateYouAttackedThisTurn(state: GameState, context: EffectContext): Boolean {
        // TODO: Track if player attacked this turn
        return false
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

    private fun evaluateTargetPowerAtMost(
        state: GameState,
        condition: TargetPowerAtMost,
        context: EffectContext
    ): Boolean {
        val target = context.targets.getOrNull(condition.targetIndex) ?: return false
        val targetEntityId = when (target) {
            is com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent -> target.entityId
            else -> return false
        }
        val card = state.getEntity(targetEntityId)?.get<CardComponent>() ?: return false
        val power = when (val p = card.baseStats?.power) {
            is com.wingedsheep.sdk.model.CharacteristicValue.Fixed -> p.value
            is com.wingedsheep.sdk.model.CharacteristicValue.Dynamic -> dynamicAmountEvaluator.evaluate(state, p.source, context)
            is com.wingedsheep.sdk.model.CharacteristicValue.DynamicWithOffset -> dynamicAmountEvaluator.evaluate(state, p.source, context) + p.offset
            null -> return false
        }
        val threshold = dynamicAmountEvaluator.evaluate(state, condition.amount, context)
        return power <= threshold
    }

    private fun evaluateTargetSpellManaValueAtMost(
        state: GameState,
        condition: TargetSpellManaValueAtMost,
        context: EffectContext
    ): Boolean {
        val target = context.targets.getOrNull(condition.targetIndex) ?: return false
        val spellEntityId = when (target) {
            is com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell -> target.spellEntityId
            else -> return false
        }
        val card = state.getEntity(spellEntityId)?.get<CardComponent>() ?: return false
        val threshold = dynamicAmountEvaluator.evaluate(state, condition.amount, context)
        return card.manaValue <= threshold
    }

    private fun evaluateEnchantedCreatureHasSubtype(
        state: GameState,
        condition: EnchantedCreatureHasSubtype,
        context: EffectContext
    ): Boolean {
        val sourceId = context.sourceId ?: return false
        val attachedId = state.getEntity(sourceId)?.get<AttachedToComponent>()?.targetId ?: return false
        return state.getEntity(attachedId)?.get<CardComponent>()?.typeLine?.hasSubtype(condition.subtype) == true
    }
}

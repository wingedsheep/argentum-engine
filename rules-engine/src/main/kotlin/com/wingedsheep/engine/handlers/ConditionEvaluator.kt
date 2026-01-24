package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.scripting.*

/**
 * Evaluates conditions from the SDK against the game state.
 */
class ConditionEvaluator {

    /**
     * Evaluate a condition and return true if met.
     */
    fun evaluate(
        state: GameState,
        condition: Condition,
        context: EffectContext
    ): Boolean {
        return when (condition) {
            // Life conditions
            is LifeTotalAtMost -> evaluateLifeAtMost(state, condition, context)
            is LifeTotalAtLeast -> evaluateLifeAtLeast(state, condition, context)
            is MoreLifeThanOpponent -> evaluateMoreLife(state, context)
            is LessLifeThanOpponent -> evaluateLessLife(state, context)

            // Battlefield conditions
            is ControlCreature -> evaluateControlCreature(state, context)
            is ControlCreaturesAtLeast -> evaluateControlCreaturesAtLeast(state, condition, context)
            is ControlCreatureWithKeyword -> evaluateControlCreatureWithKeyword(state, condition, context)
            is ControlCreatureOfType -> evaluateControlCreatureOfType(state, condition, context)
            is ControlEnchantment -> evaluateControlEnchantment(state, context)
            is ControlArtifact -> evaluateControlArtifact(state, context)
            is OpponentControlsCreature -> evaluateOpponentControlsCreature(state, context)
            is OpponentControlsMoreCreatures -> evaluateOpponentControlsMoreCreatures(state, context)
            is OpponentControlsMoreLands -> evaluateOpponentControlsMoreLands(state, context)

            // Hand conditions
            is EmptyHand -> evaluateEmptyHand(state, context)
            is CardsInHandAtLeast -> evaluateCardsInHand(state, condition, context)
            is CardsInHandAtMost -> evaluateCardsInHandAtMost(state, condition, context)

            // Graveyard conditions
            is CreatureCardsInGraveyardAtLeast -> evaluateCreatureCardsInGraveyard(state, condition, context)
            is CardsInGraveyardAtLeast -> evaluateCardsInGraveyard(state, condition, context)
            is GraveyardContainsSubtype -> evaluateGraveyardContainsSubtype(state, condition, context)

            // Source conditions
            is SourceIsAttacking -> evaluateSourceAttacking(state, context)
            is SourceIsBlocking -> evaluateSourceBlocking(state, context)
            is SourceIsTapped -> evaluateSourceTapped(state, context)
            is SourceIsUntapped -> evaluateSourceUntapped(state, context)

            // Turn conditions
            is IsYourTurn -> evaluateIsYourTurn(state, context)
            is IsNotYourTurn -> !evaluateIsYourTurn(state, context)

            // Composite conditions
            is AllConditions -> condition.conditions.all { evaluate(state, it, context) }
            is AnyCondition -> condition.conditions.any { evaluate(state, it, context) }
            is NotCondition -> !evaluate(state, condition.condition, context)
        }
    }

    private fun evaluateLifeAtMost(state: GameState, condition: LifeTotalAtMost, context: EffectContext): Boolean {
        val life = state.getEntity(context.controllerId)?.get<LifeTotalComponent>()?.life ?: return false
        return life <= condition.threshold
    }

    private fun evaluateLifeAtLeast(state: GameState, condition: LifeTotalAtLeast, context: EffectContext): Boolean {
        val life = state.getEntity(context.controllerId)?.get<LifeTotalComponent>()?.life ?: return false
        return life >= condition.threshold
    }

    private fun evaluateMoreLife(state: GameState, context: EffectContext): Boolean {
        val myLife = state.getEntity(context.controllerId)?.get<LifeTotalComponent>()?.life ?: return false
        val opponentId = context.opponentId ?: return false
        val opponentLife = state.getEntity(opponentId)?.get<LifeTotalComponent>()?.life ?: return false
        return myLife > opponentLife
    }

    private fun evaluateLessLife(state: GameState, context: EffectContext): Boolean {
        val myLife = state.getEntity(context.controllerId)?.get<LifeTotalComponent>()?.life ?: return false
        val opponentId = context.opponentId ?: return false
        val opponentLife = state.getEntity(opponentId)?.get<LifeTotalComponent>()?.life ?: return false
        return myLife < opponentLife
    }

    private fun evaluateControlCreature(state: GameState, context: EffectContext): Boolean {
        return countCreaturesControlledBy(state, context.controllerId) > 0
    }

    private fun evaluateControlCreaturesAtLeast(
        state: GameState,
        condition: ControlCreaturesAtLeast,
        context: EffectContext
    ): Boolean {
        return countCreaturesControlledBy(state, context.controllerId) >= condition.count
    }

    private fun evaluateControlCreatureWithKeyword(
        state: GameState,
        condition: ControlCreatureWithKeyword,
        context: EffectContext
    ): Boolean {
        return state.entities.any { (_, container) ->
            container.get<ControllerComponent>()?.playerId == context.controllerId &&
            container.get<CardComponent>()?.typeLine?.isCreature == true &&
            container.get<CardComponent>()?.baseKeywords?.contains(condition.keyword) == true
        }
    }

    private fun evaluateControlCreatureOfType(
        state: GameState,
        condition: ControlCreatureOfType,
        context: EffectContext
    ): Boolean {
        return state.entities.any { (_, container) ->
            container.get<ControllerComponent>()?.playerId == context.controllerId &&
            container.get<CardComponent>()?.typeLine?.hasSubtype(condition.subtype) == true
        }
    }

    private fun evaluateControlEnchantment(state: GameState, context: EffectContext): Boolean {
        return state.entities.any { (_, container) ->
            container.get<ControllerComponent>()?.playerId == context.controllerId &&
            container.get<CardComponent>()?.typeLine?.isEnchantment == true
        }
    }

    private fun evaluateControlArtifact(state: GameState, context: EffectContext): Boolean {
        return state.entities.any { (_, container) ->
            container.get<ControllerComponent>()?.playerId == context.controllerId &&
            container.get<CardComponent>()?.typeLine?.isArtifact == true
        }
    }

    private fun evaluateOpponentControlsCreature(state: GameState, context: EffectContext): Boolean {
        val opponentId = context.opponentId ?: return false
        return countCreaturesControlledBy(state, opponentId) > 0
    }

    private fun evaluateOpponentControlsMoreCreatures(state: GameState, context: EffectContext): Boolean {
        val opponentId = context.opponentId ?: return false
        val myCreatures = countCreaturesControlledBy(state, context.controllerId)
        val opponentCreatures = countCreaturesControlledBy(state, opponentId)
        return opponentCreatures > myCreatures
    }

    private fun evaluateOpponentControlsMoreLands(state: GameState, context: EffectContext): Boolean {
        val opponentId = context.opponentId ?: return false
        val myLands = countLandsControlledBy(state, context.controllerId)
        val opponentLands = countLandsControlledBy(state, opponentId)
        return opponentLands > myLands
    }

    private fun evaluateEmptyHand(state: GameState, context: EffectContext): Boolean {
        val handZone = ZoneKey(context.controllerId, ZoneType.HAND)
        return state.getZone(handZone).isEmpty()
    }

    private fun evaluateCardsInHand(state: GameState, condition: CardsInHandAtLeast, context: EffectContext): Boolean {
        val handZone = ZoneKey(context.controllerId, ZoneType.HAND)
        return state.getZone(handZone).size >= condition.count
    }

    private fun evaluateCardsInHandAtMost(state: GameState, condition: CardsInHandAtMost, context: EffectContext): Boolean {
        val handZone = ZoneKey(context.controllerId, ZoneType.HAND)
        return state.getZone(handZone).size <= condition.count
    }

    private fun evaluateCreatureCardsInGraveyard(
        state: GameState,
        condition: CreatureCardsInGraveyardAtLeast,
        context: EffectContext
    ): Boolean {
        val graveyardZone = ZoneKey(context.controllerId, ZoneType.GRAVEYARD)
        val creatureCount = state.getZone(graveyardZone).count { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.isCreature == true
        }
        return creatureCount >= condition.count
    }

    private fun evaluateCardsInGraveyard(
        state: GameState,
        condition: CardsInGraveyardAtLeast,
        context: EffectContext
    ): Boolean {
        val graveyardZone = ZoneKey(context.controllerId, ZoneType.GRAVEYARD)
        return state.getZone(graveyardZone).size >= condition.count
    }

    private fun evaluateGraveyardContainsSubtype(
        state: GameState,
        condition: GraveyardContainsSubtype,
        context: EffectContext
    ): Boolean {
        val graveyardZone = ZoneKey(context.controllerId, ZoneType.GRAVEYARD)
        return state.getZone(graveyardZone).any { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.hasSubtype(condition.subtype) == true
        }
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

    // Helper functions

    private fun countCreaturesControlledBy(state: GameState, playerId: com.wingedsheep.sdk.model.EntityId): Int {
        return state.entities.count { (_, container) ->
            container.get<ControllerComponent>()?.playerId == playerId &&
            container.get<CardComponent>()?.typeLine?.isCreature == true
        }
    }

    private fun countLandsControlledBy(state: GameState, playerId: com.wingedsheep.sdk.model.EntityId): Int {
        return state.entities.count { (_, container) ->
            container.get<ControllerComponent>()?.playerId == playerId &&
            container.get<CardComponent>()?.typeLine?.isLand == true
        }
    }
}

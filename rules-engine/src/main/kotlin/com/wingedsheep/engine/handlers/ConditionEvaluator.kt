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
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.conditions.APlayerControlsMostOfSubtype
import com.wingedsheep.sdk.scripting.conditions.AllConditions
import com.wingedsheep.sdk.scripting.conditions.AnyCondition
import com.wingedsheep.sdk.scripting.conditions.CardsInGraveyardAtLeast
import com.wingedsheep.sdk.scripting.conditions.CardsInHandAtLeast
import com.wingedsheep.sdk.scripting.conditions.CardsInHandAtMost
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.conditions.ControlArtifact
import com.wingedsheep.sdk.scripting.conditions.ControlCreature
import com.wingedsheep.sdk.scripting.conditions.ControlCreatureOfType
import com.wingedsheep.sdk.scripting.conditions.ControlCreatureWithKeyword
import com.wingedsheep.sdk.scripting.conditions.ControlCreaturesAtLeast
import com.wingedsheep.sdk.scripting.conditions.ControlEnchantment
import com.wingedsheep.sdk.scripting.conditions.CreatureCardsInGraveyardAtLeast
import com.wingedsheep.sdk.scripting.conditions.EmptyHand
import com.wingedsheep.sdk.scripting.conditions.EnchantedCreatureHasSubtype
import com.wingedsheep.sdk.scripting.conditions.GraveyardContainsSubtype
import com.wingedsheep.sdk.scripting.conditions.IsNotYourTurn
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn
import com.wingedsheep.sdk.scripting.conditions.LessLifeThanOpponent
import com.wingedsheep.sdk.scripting.conditions.LifeTotalAtLeast
import com.wingedsheep.sdk.scripting.conditions.LifeTotalAtMost
import com.wingedsheep.sdk.scripting.conditions.MoreLifeThanOpponent
import com.wingedsheep.sdk.scripting.conditions.NotCondition
import com.wingedsheep.sdk.scripting.conditions.OpponentControlsCreature
import com.wingedsheep.sdk.scripting.conditions.OpponentControlsMoreCreatures
import com.wingedsheep.sdk.scripting.conditions.OpponentControlsMoreLands
import com.wingedsheep.sdk.scripting.conditions.OpponentSpellOnStack
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
            is APlayerControlsMostOfSubtype -> evaluateAPlayerControlsMostOfSubtype(state, condition)
            is TargetPowerAtMost -> evaluateTargetPowerAtMost(state, condition, context)
            is TargetSpellManaValueAtMost -> evaluateTargetSpellManaValueAtMost(state, condition, context)
            is EnchantedCreatureHasSubtype -> evaluateEnchantedCreatureHasSubtype(state, condition, context)

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
            is SourceEnteredThisTurn -> evaluateSourceEnteredThisTurn(state, context)
            is SourceHasDealtDamage -> evaluateSourceHasDealtDamage(state, context)
            is SourceHasDealtCombatDamageToPlayer -> evaluateSourceHasDealtCombatDamageToPlayer(state, context)
            is SourceHasSubtype -> evaluateSourceHasSubtype(state, condition, context)

            // Turn conditions
            is IsYourTurn -> evaluateIsYourTurn(state, context)
            is IsNotYourTurn -> !evaluateIsYourTurn(state, context)
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
        val handZone = ZoneKey(context.controllerId, Zone.HAND)
        return state.getZone(handZone).isEmpty()
    }

    private fun evaluateCardsInHand(state: GameState, condition: CardsInHandAtLeast, context: EffectContext): Boolean {
        val handZone = ZoneKey(context.controllerId, Zone.HAND)
        return state.getZone(handZone).size >= condition.count
    }

    private fun evaluateCardsInHandAtMost(state: GameState, condition: CardsInHandAtMost, context: EffectContext): Boolean {
        val handZone = ZoneKey(context.controllerId, Zone.HAND)
        return state.getZone(handZone).size <= condition.count
    }

    private fun evaluateCreatureCardsInGraveyard(
        state: GameState,
        condition: CreatureCardsInGraveyardAtLeast,
        context: EffectContext
    ): Boolean {
        val graveyardZone = ZoneKey(context.controllerId, Zone.GRAVEYARD)
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
        val graveyardZone = ZoneKey(context.controllerId, Zone.GRAVEYARD)
        return state.getZone(graveyardZone).size >= condition.count
    }

    private fun evaluateGraveyardContainsSubtype(
        state: GameState,
        condition: GraveyardContainsSubtype,
        context: EffectContext
    ): Boolean {
        val graveyardZone = ZoneKey(context.controllerId, Zone.GRAVEYARD)
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

    private fun evaluateSourceEnteredThisTurn(state: GameState, context: EffectContext): Boolean {
        // TODO: Track when permanents entered the battlefield
        // For now, return false as we don't have this tracking yet
        return false
    }

    private fun evaluateSourceHasDealtDamage(state: GameState, context: EffectContext): Boolean {
        // TODO: Track damage dealt by source this turn
        // For now, return false as we don't have this tracking yet
        return false
    }

    private fun evaluateSourceHasSubtype(state: GameState, condition: SourceHasSubtype, context: EffectContext): Boolean {
        val sourceId = context.sourceId ?: return false
        return state.getEntity(sourceId)?.get<CardComponent>()?.typeLine?.hasSubtype(condition.subtype) == true
    }

    private fun evaluateSourceHasDealtCombatDamageToPlayer(state: GameState, context: EffectContext): Boolean {
        // TODO: Track combat damage dealt to players
        // For now, return false as we don't have this tracking yet
        return false
    }

    private fun evaluateYouAttackedThisTurn(state: GameState, context: EffectContext): Boolean {
        // TODO: Track if player attacked this turn
        // For now, return false as we don't have this tracking yet
        return false
    }

    private fun evaluateYouWereAttackedThisStep(state: GameState, context: EffectContext): Boolean {
        // Check if any creature is attacking the player (has AttackingComponent with defenderId = player)
        return state.entities.any { (_, container) ->
            val attacking = container.get<AttackingComponent>()
            attacking != null && attacking.defenderId == context.controllerId
        }
    }

    private fun evaluateYouWereDealtCombatDamageThisTurn(state: GameState, context: EffectContext): Boolean {
        // TODO: Track if player was dealt combat damage this turn
        // For now, return false as we don't have this tracking yet
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

    // Helper functions

    private fun countCreaturesControlledBy(state: GameState, playerId: com.wingedsheep.sdk.model.EntityId): Int {
        val battlefieldEntities = state.getBattlefield().toSet()
        return battlefieldEntities.count { entityId ->
            val container = state.getEntity(entityId) ?: return@count false
            container.get<ControllerComponent>()?.playerId == playerId &&
            container.get<CardComponent>()?.typeLine?.isCreature == true
        }
    }

    private fun countLandsControlledBy(state: GameState, playerId: com.wingedsheep.sdk.model.EntityId): Int {
        val battlefieldEntities = state.getBattlefield().toSet()
        return battlefieldEntities.count { entityId ->
            val container = state.getEntity(entityId) ?: return@count false
            container.get<ControllerComponent>()?.playerId == playerId &&
            container.get<CardComponent>()?.typeLine?.isLand == true
        }
    }
}

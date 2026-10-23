package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceStatics
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.RollAdditionalDice

/**
 * Die-roll replacements, consulted by
 * [com.wingedsheep.engine.handlers.effects.composite.RollDiceExecutor] — the single place a die is
 * actually rolled — so every roll in the game runs through the same filter.
 */
object DiceRollModifiers {

    /**
     * How many additional dice [playerId] rolls, and how many of their lowest rolls they ignore,
     * in place of an instruction to roll one or more dice — the sum of [RollAdditionalDice.extraDice]
     * across the permanents they control (instances add, per the Barbarian Class ruling).
     *
     * Uses projected controllers and abilities, so a stolen Pixie Guide helps its current controller
     * and one that lost its abilities helps nobody.
     */
    fun extraDice(state: GameState, cardRegistry: CardRegistry, playerId: EntityId): Int {
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(playerId).sumOf { permanentId ->
            val entity = state.getEntity(permanentId) ?: return@sumOf 0
            if (entity.has<FaceDownComponent>() || projected.hasLostAllAbilities(permanentId)) return@sumOf 0
            val cardDef = entity.get<CardComponent>()?.let { cardRegistry.getCard(it.cardDefinitionId) } ?: return@sumOf 0
            RoomFaceStatics.activeStaticAbilities(entity, cardDef)
                .filterIsInstance<RollAdditionalDice>()
                .sumOf { it.extraDice.coerceAtLeast(0) }
        }
    }
}

package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.GrantPlayWithoutPayingCostEffect
import kotlin.reflect.KClass

/**
 * Executor for GrantMayPlayFromExileEffect.
 *
 * Marks all cards in a named collection with MayPlayFromExileComponent,
 * granting the controller permission to play them from exile until end of turn.
 */
class GrantMayPlayFromExileExecutor : EffectExecutor<GrantMayPlayFromExileEffect> {

    override val effectType: KClass<GrantMayPlayFromExileEffect> = GrantMayPlayFromExileEffect::class

    override fun execute(
        state: GameState,
        effect: GrantMayPlayFromExileEffect,
        context: EffectContext
    ): EffectResult {
        val controllerId = context.controllerId
        val collection = context.pipeline.storedCollections[effect.from] ?: emptyList()

        val expiresAfterTurn = if (effect.untilEndOfNextTurn) {
            calculateNextTurnOfPlayer(state, controllerId)
        } else {
            null
        }

        var newState = state
        for (cardId in collection) {
            newState = newState.updateEntity(cardId) { container ->
                container.with(
                    MayPlayFromExileComponent(
                        controllerId = controllerId,
                        permanent = effect.permanent,
                        expiresAfterTurn = expiresAfterTurn,
                        withAnyManaType = effect.withAnyManaType
                    )
                )
            }
        }

        return EffectResult.success(newState)
    }

    /**
     * Calculate the turn number of the player's next turn.
     * If it's currently the player's turn, "next turn" means their following turn.
     */
    private fun calculateNextTurnOfPlayer(state: GameState, playerId: com.wingedsheep.sdk.model.EntityId): Int {
        val turnOrder = state.turnOrder
        val playerIndex = turnOrder.indexOf(playerId)
        val activeIndex = turnOrder.indexOf(state.activePlayerId)
        val playerCount = turnOrder.size

        // How many turns until it's this player's turn again
        val turnsUntilNext = if (playerIndex == activeIndex) {
            // It's currently their turn — "next turn" is playerCount turns away
            playerCount
        } else {
            // Calculate distance forward in turn order
            (playerIndex - activeIndex + playerCount) % playerCount
        }

        return state.turnNumber + turnsUntilNext
    }
}

/**
 * Executor for GrantPlayWithoutPayingCostEffect.
 *
 * Marks all cards in a named collection with PlayWithoutPayingCostComponent,
 * allowing the controller to play them without paying mana cost until end of turn.
 */
class GrantPlayWithoutPayingCostExecutor : EffectExecutor<GrantPlayWithoutPayingCostEffect> {

    override val effectType: KClass<GrantPlayWithoutPayingCostEffect> = GrantPlayWithoutPayingCostEffect::class

    override fun execute(
        state: GameState,
        effect: GrantPlayWithoutPayingCostEffect,
        context: EffectContext
    ): EffectResult {
        val controllerId = context.controllerId
        val collection = context.pipeline.storedCollections[effect.from] ?: emptyList()

        var newState = state
        for (cardId in collection) {
            newState = newState.updateEntity(cardId) { container ->
                container.with(PlayWithoutPayingCostComponent(controllerId = controllerId))
            }
        }

        return EffectResult.success(newState)
    }
}

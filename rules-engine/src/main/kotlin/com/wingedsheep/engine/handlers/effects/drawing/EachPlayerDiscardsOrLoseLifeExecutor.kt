package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EachPlayerDiscardsOrLoseLifeContinuation
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EachPlayerDiscardsOrLoseLifeEffect
import kotlin.reflect.KClass

/**
 * Executor for EachPlayerDiscardsOrLoseLifeEffect.
 *
 * Handles "Each player discards a card. Then each player who didn't discard
 * a creature card this way loses N life." (Strongarm Tactics)
 *
 * Players discard in APNAP order. After all players have discarded,
 * life loss is applied to those who didn't discard a creature card.
 * Players with empty hands are treated as not discarding a creature.
 */
class EachPlayerDiscardsOrLoseLifeExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<EachPlayerDiscardsOrLoseLifeEffect> {

    override val effectType: KClass<EachPlayerDiscardsOrLoseLifeEffect> = EachPlayerDiscardsOrLoseLifeEffect::class

    override fun execute(
        state: GameState,
        effect: EachPlayerDiscardsOrLoseLifeEffect,
        context: EffectContext
    ): ExecutionResult {
        val activePlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")

        val playerOrder = listOf(activePlayer) + state.turnOrder.filter { it != activePlayer }

        return askPlayerToDiscard(
            state = state,
            context = context,
            playerOrder = playerOrder,
            currentPlayerIndex = 0,
            discardedCreature = emptyMap(),
            lifeLoss = effect.lifeLoss
        )
    }

    private fun askPlayerToDiscard(
        state: GameState,
        context: EffectContext,
        playerOrder: List<EntityId>,
        currentPlayerIndex: Int,
        discardedCreature: Map<EntityId, Boolean>,
        lifeLoss: Int
    ): ExecutionResult {
        val playerId = playerOrder[currentPlayerIndex]
        val handZone = ZoneKey(playerId, Zone.HAND)
        val hand = state.getZone(handZone)

        // If hand is empty, this player didn't discard a creature
        if (hand.isEmpty()) {
            val newDiscardedCreature = discardedCreature + (playerId to false)
            return proceedToNextPlayerOrFinish(
                state = state,
                context = context,
                playerOrder = playerOrder,
                currentPlayerIndex = currentPlayerIndex,
                discardedCreature = newDiscardedCreature,
                lifeLoss = lifeLoss
            )
        }

        // If hand has exactly 1 card, auto-discard it
        if (hand.size == 1) {
            val cardId = hand.first()
            val isCreature = state.getEntity(cardId)?.get<CardComponent>()?.isCreature == true

            val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
            var newState = state.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)

            val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Card"
            val events = listOf(com.wingedsheep.engine.core.CardsDiscardedEvent(playerId, listOf(cardId), listOf(cardName)))
            val newDiscardedCreature = discardedCreature + (playerId to isCreature)

            val nextIndex = currentPlayerIndex + 1
            return if (nextIndex < playerOrder.size) {
                // Continue asking next player, carrying events forward
                val nextResult = askPlayerToDiscard(
                    state = newState,
                    context = context,
                    playerOrder = playerOrder,
                    currentPlayerIndex = nextIndex,
                    discardedCreature = newDiscardedCreature,
                    lifeLoss = lifeLoss
                )
                ExecutionResult(nextResult.state, events + nextResult.events, nextResult.error)
            } else {
                // All done, apply life loss
                val lifeLossResult = applyLifeLoss(newState, newDiscardedCreature, lifeLoss)
                ExecutionResult(lifeLossResult.state, events + lifeLossResult.events, lifeLossResult.error)
            }
        }

        // Player must choose which card to discard
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = playerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            prompt = "Choose a card to discard",
            options = hand,
            minSelections = 1,
            maxSelections = 1,
            ordered = false,
            phase = DecisionPhase.RESOLUTION
        )

        val remainingPlayers = playerOrder.drop(currentPlayerIndex + 1)

        val continuation = EachPlayerDiscardsOrLoseLifeContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            sourceId = context.sourceId,
            sourceName = sourceName,
            controllerId = context.controllerId,
            currentPlayerId = playerId,
            remainingPlayers = remainingPlayers,
            discardedCreature = discardedCreature,
            lifeLoss = lifeLoss
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    private fun proceedToNextPlayerOrFinish(
        state: GameState,
        context: EffectContext,
        playerOrder: List<EntityId>,
        currentPlayerIndex: Int,
        discardedCreature: Map<EntityId, Boolean>,
        lifeLoss: Int
    ): ExecutionResult {
        val nextIndex = currentPlayerIndex + 1
        return if (nextIndex < playerOrder.size) {
            askPlayerToDiscard(
                state = state,
                context = context,
                playerOrder = playerOrder,
                currentPlayerIndex = nextIndex,
                discardedCreature = discardedCreature,
                lifeLoss = lifeLoss
            )
        } else {
            applyLifeLoss(state, discardedCreature, lifeLoss)
        }
    }

    companion object {
        /**
         * Apply life loss to all players who didn't discard a creature card.
         */
        fun applyLifeLoss(
            state: GameState,
            discardedCreature: Map<EntityId, Boolean>,
            lifeLoss: Int
        ): ExecutionResult {
            var currentState = state
            val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

            for ((playerId, discardedCreatureCard) in discardedCreature) {
                if (!discardedCreatureCard) {
                    val currentLife = currentState.getEntity(playerId)
                        ?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()?.life
                        ?: continue
                    val newLife = currentLife - lifeLoss
                    currentState = currentState.updateEntity(playerId) { container ->
                        container.with(com.wingedsheep.engine.state.components.identity.LifeTotalComponent(newLife))
                    }
                    events.add(
                        com.wingedsheep.engine.core.LifeChangedEvent(
                            playerId, currentLife, newLife,
                            com.wingedsheep.engine.core.LifeChangeReason.LIFE_LOSS
                        )
                    )
                }
            }

            return ExecutionResult.success(currentState, events)
        }
    }
}

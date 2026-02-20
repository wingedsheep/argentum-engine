package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.EachPlayerMayDrawEffect
import kotlin.reflect.KClass

/**
 * Executor for EachPlayerMayDrawEffect.
 *
 * Handles effects like Temporary Truce where each player chooses how many cards
 * (0 to maxCards) to draw, and gains life for each card not drawn.
 *
 * "Each player may draw up to two cards. For each card less than two a player
 * draws this way, that player gains 2 life."
 *
 * The executor creates a decision for the first player (in APNAP order),
 * then pushes a continuation to handle subsequent players and the final
 * draws/life gains.
 */
class EachPlayerMayDrawExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<EachPlayerMayDrawEffect> {

    override val effectType: KClass<EachPlayerMayDrawEffect> = EachPlayerMayDrawEffect::class

    override fun execute(
        state: GameState,
        effect: EachPlayerMayDrawEffect,
        context: EffectContext
    ): ExecutionResult {
        // Get players in APNAP order (active player first)
        val activePlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")

        val playerOrder = listOf(activePlayer) + state.turnOrder.filter { it != activePlayer }

        // Start with the first player
        return askPlayerToChoose(
            state = state,
            effect = effect,
            context = context,
            playerOrder = playerOrder,
            currentPlayerIndex = 0,
            drawAmounts = emptyMap(),
            lifeGainAmounts = emptyMap()
        )
    }

    /**
     * Create a decision for a player to choose how many cards to draw.
     */
    private fun askPlayerToChoose(
        state: GameState,
        effect: EachPlayerMayDrawEffect,
        context: EffectContext,
        playerOrder: List<EntityId>,
        currentPlayerIndex: Int,
        drawAmounts: Map<EntityId, Int>,
        lifeGainAmounts: Map<EntityId, Int>
    ): ExecutionResult {
        val playerId = playerOrder[currentPlayerIndex]
        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        val librarySize = state.getZone(libraryZone).size

        // Get source name for the prompt
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        // Calculate the actual max cards (can't draw more than library size)
        val actualMax = effect.maxCards.coerceAtMost(librarySize)

        val prompt = if (effect.lifePerCardNotDrawn > 0) {
            "Choose how many cards to draw (0-$actualMax). Gain ${effect.lifePerCardNotDrawn} life for each card not drawn."
        } else {
            "Choose how many cards to draw (0-$actualMax)"
        }

        // Create number choice decision
        val decisionResult = decisionHandler.createNumberDecision(
            state = state,
            playerId = playerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            prompt = prompt,
            minValue = 0,
            maxValue = actualMax,
            phase = DecisionPhase.RESOLUTION
        )

        // Build continuation for remaining players
        val remainingPlayers = playerOrder.drop(currentPlayerIndex + 1)

        val continuation = EachPlayerChoosesDrawContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            sourceId = context.sourceId,
            sourceName = sourceName,
            controllerId = context.controllerId,
            currentPlayerId = playerId,
            remainingPlayers = remainingPlayers,
            drawAmounts = drawAmounts,
            lifeGainAmounts = lifeGainAmounts,
            maxCards = effect.maxCards,
            lifePerCardNotDrawn = effect.lifePerCardNotDrawn
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    companion object {
        /**
         * Execute draws and life gains for all players based on their choices.
         * Called after all players have made their selections.
         */
        fun executeDrawsAndLifeGains(
            state: GameState,
            drawAmounts: Map<EntityId, Int>,
            lifeGainAmounts: Map<EntityId, Int>
        ): ExecutionResult {
            var currentState = state
            val allEvents = mutableListOf<GameEvent>()

            // Draw cards for each player (in order they appear in drawAmounts)
            for ((playerId, count) in drawAmounts) {
                if (count > 0) {
                    val drawResult = drawCards(currentState, playerId, count)
                    currentState = drawResult.state
                    allEvents.addAll(drawResult.events)

                    // Check for draw failure (empty library)
                    if (!drawResult.isSuccess) {
                        return ExecutionResult(currentState, allEvents, drawResult.error)
                    }
                }
            }

            // Gain life for each player
            for ((playerId, amount) in lifeGainAmounts) {
                if (amount > 0) {
                    val lifeResult = gainLife(currentState, playerId, amount)
                    currentState = lifeResult.state
                    allEvents.addAll(lifeResult.events)
                }
            }

            return ExecutionResult.success(currentState, allEvents)
        }

        /**
         * Draw cards for a player.
         */
        private fun drawCards(
            state: GameState,
            playerId: EntityId,
            count: Int
        ): ExecutionResult {
            var newState = state
            val drawnCards = mutableListOf<EntityId>()

            val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
            val handZone = ZoneKey(playerId, Zone.HAND)

            repeat(count) {
                val library = newState.getZone(libraryZone)
                if (library.isEmpty()) {
                    // Failed to draw - game loss condition (Rule 704.5c)
                    newState = newState.updateEntity(playerId) { container ->
                        container.with(PlayerLostComponent(LossReason.EMPTY_LIBRARY))
                    }
                    return ExecutionResult.success(
                        newState,
                        listOf(DrawFailedEvent(playerId, "Empty library"))
                    )
                }

                val cardId = library.first()
                drawnCards.add(cardId)

                newState = newState.removeFromZone(libraryZone, cardId)
                newState = newState.addToZone(handZone, cardId)
            }

            val cardNames = drawnCards.map { newState.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
            return ExecutionResult.success(
                newState,
                listOf(CardsDrawnEvent(playerId, drawnCards.size, drawnCards, cardNames))
            )
        }

        /**
         * Gain life for a player.
         */
        private fun gainLife(
            state: GameState,
            playerId: EntityId,
            amount: Int
        ): ExecutionResult {
            val currentLife = state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 0
            val newLife = currentLife + amount

            val newState = state.updateEntity(playerId) { container ->
                container.with(LifeTotalComponent(newLife))
            }

            return ExecutionResult.success(
                newState,
                listOf(LifeChangedEvent(playerId, currentLife, newLife, LifeChangeReason.LIFE_GAIN))
            )
        }
    }
}

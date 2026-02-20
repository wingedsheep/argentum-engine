package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.TradeSecretsEffect
import kotlin.reflect.KClass

/**
 * Executor for TradeSecretsEffect.
 *
 * "Target opponent draws two cards, then you draw up to four cards.
 * That opponent may repeat this process as many times as they choose."
 *
 * Flow:
 * 1. Opponent draws 2 cards
 * 2. Controller chooses how many cards to draw (0-4) → ChooseNumberDecision
 * 3. Opponent decides whether to repeat → YesNoDecision
 * 4. If yes → go back to step 1; if no → done
 */
class TradeSecretsExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<TradeSecretsEffect> {

    override val effectType: KClass<TradeSecretsEffect> = TradeSecretsEffect::class

    override fun execute(
        state: GameState,
        effect: TradeSecretsEffect,
        context: EffectContext
    ): ExecutionResult {
        val controllerId = context.controllerId
        val opponentId = context.opponentId
            ?: return ExecutionResult.error(state, "Trade Secrets requires a target opponent")

        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        // Step 1: Opponent draws 2 cards
        val drawResult = drawCards(state, opponentId, 2)
        if (!drawResult.isSuccess) {
            return drawResult
        }

        // Step 2: Ask controller how many cards to draw (0-4)
        return askControllerToDraw(
            state = drawResult.state,
            controllerId = controllerId,
            opponentId = opponentId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            priorEvents = drawResult.events.toList()
        )
    }

    companion object {
        /**
         * Ask the controller to choose how many cards to draw (0 to 4).
         */
        fun askControllerToDraw(
            state: GameState,
            controllerId: EntityId,
            opponentId: EntityId,
            sourceId: EntityId?,
            sourceName: String?,
            priorEvents: List<GameEvent>
        ): ExecutionResult {
            val libraryZone = ZoneKey(controllerId, Zone.LIBRARY)
            val librarySize = state.getZone(libraryZone).size
            val actualMax = 4.coerceAtMost(librarySize)

            val decisionHandler = DecisionHandler()
            val decisionResult = decisionHandler.createNumberDecision(
                state = state,
                playerId = controllerId,
                sourceId = sourceId,
                sourceName = sourceName,
                prompt = "Choose how many cards to draw (0-$actualMax)",
                minValue = 0,
                maxValue = actualMax,
                phase = DecisionPhase.RESOLUTION
            )

            val continuation = TradeSecretsContinuation(
                decisionId = decisionResult.pendingDecision!!.id,
                controllerId = controllerId,
                opponentId = opponentId,
                sourceId = sourceId,
                sourceName = sourceName,
                phase = TradeSecretsPhase.CONTROLLER_DRAWS
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                priorEvents + decisionResult.events
            )
        }

        /**
         * Ask the opponent whether to repeat the process.
         */
        fun askOpponentToRepeat(
            state: GameState,
            controllerId: EntityId,
            opponentId: EntityId,
            sourceId: EntityId?,
            sourceName: String?,
            priorEvents: List<GameEvent>
        ): ExecutionResult {
            val decisionHandler = DecisionHandler()
            val decisionResult = decisionHandler.createYesNoDecision(
                state = state,
                playerId = opponentId,
                sourceId = sourceId,
                sourceName = sourceName,
                prompt = "Repeat the process? (You draw 2 cards, opponent draws up to 4)",
                yesText = "Repeat",
                noText = "Stop",
                phase = DecisionPhase.RESOLUTION
            )

            val continuation = TradeSecretsContinuation(
                decisionId = decisionResult.pendingDecision!!.id,
                controllerId = controllerId,
                opponentId = opponentId,
                sourceId = sourceId,
                sourceName = sourceName,
                phase = TradeSecretsPhase.OPPONENT_REPEATS
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                priorEvents + decisionResult.events
            )
        }

        /**
         * Draw cards for a player.
         */
        fun drawCards(
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
    }
}

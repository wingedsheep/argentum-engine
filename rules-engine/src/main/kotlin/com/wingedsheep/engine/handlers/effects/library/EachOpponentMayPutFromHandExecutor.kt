package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EachOpponentMayPutFromHandEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import kotlin.reflect.KClass

/**
 * Executor for EachOpponentMayPutFromHandEffect.
 *
 * Each opponent in APNAP order may put any number of cards matching the filter
 * from their hand onto the battlefield.
 *
 * Example: Tempting Wurm - "Each opponent may put any number of artifact, creature,
 * enchantment, and/or land cards from their hand onto the battlefield."
 */
class EachOpponentMayPutFromHandExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<EachOpponentMayPutFromHandEffect> {

    override val effectType: KClass<EachOpponentMayPutFromHandEffect> = EachOpponentMayPutFromHandEffect::class

    private val predicateEvaluator = PredicateEvaluator()

    override fun execute(
        state: GameState,
        effect: EachOpponentMayPutFromHandEffect,
        context: EffectContext
    ): ExecutionResult {
        val controllerId = context.controllerId

        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        // Get opponents in APNAP order (active player first, then turn order)
        val activePlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")
        val apnapOrder = listOf(activePlayer) + state.turnOrder.filter { it != activePlayer }
        val opponents = apnapOrder.filter { it != controllerId }

        if (opponents.isEmpty()) {
            return ExecutionResult.success(state)
        }

        return askNextOpponent(state, effect.filter, context.sourceId, sourceName, controllerId, opponents, 0)
    }

    companion object {
        private val predicateEvaluator = PredicateEvaluator()

        /**
         * Ask the next opponent to select cards from their hand.
         * Skips opponents who have no matching cards in hand.
         */
        fun askNextOpponent(
            state: GameState,
            filter: GameObjectFilter,
            sourceId: EntityId?,
            sourceName: String?,
            controllerId: EntityId,
            opponents: List<EntityId>,
            currentIndex: Int,
            decisionHandler: DecisionHandler = DecisionHandler()
        ): ExecutionResult {
            var index = currentIndex
            while (index < opponents.size) {
                val opponentId = opponents[index]
                val matchingCards = findMatchingCardsInHand(state, opponentId, filter)
                if (matchingCards.isNotEmpty()) {
                    break
                }
                index++
            }

            // No more opponents with matching cards
            if (index >= opponents.size) {
                return ExecutionResult.success(state)
            }

            val opponentId = opponents[index]
            val matchingCards = findMatchingCardsInHand(state, opponentId, filter)

            val prompt = "You may put any number of ${filter.description} cards from your hand onto the battlefield"

            val decisionResult = decisionHandler.createCardSelectionDecision(
                state = state,
                playerId = opponentId,
                sourceId = sourceId,
                sourceName = sourceName,
                prompt = prompt,
                options = matchingCards,
                minSelections = 0,
                maxSelections = matchingCards.size,
                ordered = false,
                phase = DecisionPhase.RESOLUTION
            )

            val continuation = EachOpponentMayPutFromHandContinuation(
                decisionId = decisionResult.pendingDecision!!.id,
                currentOpponentId = opponentId,
                remainingOpponents = opponents.drop(index + 1),
                sourceId = sourceId,
                sourceName = sourceName,
                controllerId = controllerId,
                filter = filter
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                decisionResult.events
            )
        }

        fun findMatchingCardsInHand(
            state: GameState,
            playerId: EntityId,
            filter: GameObjectFilter
        ): List<EntityId> {
            val handZone = ZoneKey(playerId, Zone.HAND)
            val hand = state.getZone(handZone)
            val context = PredicateContext(controllerId = playerId)

            return hand.filter { cardId ->
                predicateEvaluator.matches(state, cardId, filter, context)
            }
        }
    }
}

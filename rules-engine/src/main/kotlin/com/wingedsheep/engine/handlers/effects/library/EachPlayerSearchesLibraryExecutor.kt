package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EachPlayerSearchesLibraryEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import kotlin.reflect.KClass

/**
 * Executor for EachPlayerSearchesLibraryEffect.
 *
 * Each player in APNAP order may search their library for up to X cards matching a filter,
 * reveal those cards, put them into their hand, then shuffle.
 *
 * Example: Weird Harvest - "Each player may search their library for up to X creature cards,
 * reveal those cards, put them into their hand, then shuffle."
 */
class EachPlayerSearchesLibraryExecutor : EffectExecutor<EachPlayerSearchesLibraryEffect> {

    override val effectType: KClass<EachPlayerSearchesLibraryEffect> = EachPlayerSearchesLibraryEffect::class

    private val amountEvaluator = DynamicAmountEvaluator()

    override fun execute(
        state: GameState,
        effect: EachPlayerSearchesLibraryEffect,
        context: EffectContext
    ): ExecutionResult {
        val activePlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")
        val apnapOrder = listOf(activePlayer) + state.turnOrder.filter { it != activePlayer }

        val maxCount = amountEvaluator.evaluate(state, effect.count, context)
        if (maxCount <= 0) {
            return ExecutionResult.success(state)
        }

        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        return askNextPlayer(
            state = state,
            sourceId = context.sourceId,
            sourceName = sourceName,
            players = apnapOrder,
            currentIndex = 0,
            filter = effect.filter,
            maxCount = maxCount
        )
    }

    companion object {
        private val predicateEvaluator = PredicateEvaluator()

        fun askNextPlayer(
            state: GameState,
            sourceId: EntityId?,
            sourceName: String?,
            players: List<EntityId>,
            currentIndex: Int,
            filter: GameObjectFilter,
            maxCount: Int
        ): ExecutionResult {
            // Skip players with empty libraries or no matching cards
            var index = currentIndex
            while (index < players.size) {
                val playerId = players[index]
                val matchingCards = findMatchingCardsInLibrary(state, playerId, filter)
                if (matchingCards.isNotEmpty()) {
                    break
                }
                // Still need to shuffle even if no matches
                index++
            }

            // All players done
            if (index >= players.size) {
                return ExecutionResult.success(state)
            }

            val playerId = players[index]
            val matchingCards = findMatchingCardsInLibrary(state, playerId, filter)

            val maxSelections = minOf(maxCount, matchingCards.size)

            // Build card info map for the UI
            val cardInfoMap = matchingCards.associateWith { cardId ->
                val container = state.getEntity(cardId)
                val cardComponent = container?.get<CardComponent>()
                SearchCardInfo(
                    name = cardComponent?.name ?: "Unknown",
                    manaCost = cardComponent?.manaCost?.toString() ?: "",
                    typeLine = cardComponent?.typeLine?.toString() ?: "",
                    imageUri = null
                )
            }

            val decisionId = java.util.UUID.randomUUID().toString()
            val filterDescription = filter.description

            val decision = SearchLibraryDecision(
                id = decisionId,
                playerId = playerId,
                prompt = "Search your library for up to $maxCount $filterDescription card${if (maxCount != 1) "s" else ""}",
                context = DecisionContext(
                    sourceId = sourceId,
                    sourceName = sourceName,
                    phase = DecisionPhase.RESOLUTION
                ),
                options = matchingCards,
                minSelections = 0,
                maxSelections = maxSelections,
                cards = cardInfoMap,
                filterDescription = filterDescription
            )

            val continuation = EachPlayerSearchesLibraryContinuation(
                decisionId = decisionId,
                currentPlayerId = playerId,
                remainingPlayers = players.drop(index + 1),
                sourceId = sourceId,
                sourceName = sourceName,
                filter = filter,
                maxCount = maxCount
            )

            val stateWithDecision = state.withPendingDecision(decision)
            val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decision,
                listOf(
                    DecisionRequestedEvent(
                        decisionId = decisionId,
                        playerId = playerId,
                        decisionType = "SEARCH_LIBRARY",
                        prompt = decision.prompt
                    )
                )
            )
        }

        fun findMatchingCardsInLibrary(
            state: GameState,
            playerId: EntityId,
            filter: GameObjectFilter
        ): List<EntityId> {
            val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
            val library = state.getZone(libraryZone)
            val context = PredicateContext(controllerId = playerId)
            return library.filter { cardId ->
                predicateEvaluator.matches(state, cardId, filter, context)
            }
        }
    }
}

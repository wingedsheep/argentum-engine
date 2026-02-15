package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EachPlayerMayRevealCreaturesEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import kotlin.reflect.KClass

/**
 * Executor for EachPlayerMayRevealCreaturesEffect.
 *
 * Each player in APNAP order may reveal any number of creature cards from their hand.
 * Then each player creates tokens for each card they revealed.
 *
 * Example: Kamahl's Summons - "Each player may reveal any number of creature cards
 * from their hand. Then each player creates a 2/2 green Bear creature token for
 * each card they revealed this way."
 */
class EachPlayerMayRevealCreaturesExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<EachPlayerMayRevealCreaturesEffect> {

    override val effectType: KClass<EachPlayerMayRevealCreaturesEffect> = EachPlayerMayRevealCreaturesEffect::class

    override fun execute(
        state: GameState,
        effect: EachPlayerMayRevealCreaturesEffect,
        context: EffectContext
    ): ExecutionResult {
        // Get all players in APNAP order (active player first, then turn order)
        val activePlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")
        val apnapOrder = listOf(activePlayer) + state.turnOrder.filter { it != activePlayer }

        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        return askNextPlayer(
            state = state,
            sourceId = context.sourceId,
            sourceName = sourceName,
            players = apnapOrder,
            currentIndex = 0,
            revealCounts = emptyMap(),
            tokenPower = effect.tokenPower,
            tokenToughness = effect.tokenToughness,
            tokenColors = effect.tokenColors,
            tokenCreatureTypes = effect.tokenCreatureTypes
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
            revealCounts: Map<EntityId, Int>,
            tokenPower: Int,
            tokenToughness: Int,
            tokenColors: Set<Color>,
            tokenCreatureTypes: Set<String>,
            decisionHandler: DecisionHandler = DecisionHandler()
        ): ExecutionResult {
            var index = currentIndex
            while (index < players.size) {
                val playerId = players[index]
                val matchingCards = findCreatureCardsInHand(state, playerId)
                if (matchingCards.isNotEmpty()) {
                    break
                }
                index++
            }

            // No more players with creature cards - create tokens for everyone
            if (index >= players.size) {
                return createTokensForAllPlayers(state, revealCounts, tokenPower, tokenToughness, tokenColors, tokenCreatureTypes)
            }

            val playerId = players[index]
            val matchingCards = findCreatureCardsInHand(state, playerId)

            val prompt = "You may reveal any number of creature cards from your hand"

            val decisionResult = decisionHandler.createCardSelectionDecision(
                state = state,
                playerId = playerId,
                sourceId = sourceId,
                sourceName = sourceName,
                prompt = prompt,
                options = matchingCards,
                minSelections = 0,
                maxSelections = matchingCards.size,
                ordered = false,
                phase = DecisionPhase.RESOLUTION
            )

            val continuation = EachPlayerMayRevealCreaturesContinuation(
                decisionId = decisionResult.pendingDecision!!.id,
                currentPlayerId = playerId,
                remainingPlayers = players.drop(index + 1),
                sourceId = sourceId,
                sourceName = sourceName,
                revealCounts = revealCounts,
                tokenPower = tokenPower,
                tokenToughness = tokenToughness,
                tokenColors = tokenColors,
                tokenCreatureTypes = tokenCreatureTypes
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                decisionResult.events
            )
        }

        fun findCreatureCardsInHand(
            state: GameState,
            playerId: EntityId
        ): List<EntityId> {
            val handZone = ZoneKey(playerId, Zone.HAND)
            val hand = state.getZone(handZone)
            val context = PredicateContext(controllerId = playerId)

            return hand.filter { cardId ->
                predicateEvaluator.matches(state, cardId, GameObjectFilter.Creature, context)
            }
        }

        fun createTokensForAllPlayers(
            state: GameState,
            revealCounts: Map<EntityId, Int>,
            tokenPower: Int,
            tokenToughness: Int,
            tokenColors: Set<Color>,
            tokenCreatureTypes: Set<String>
        ): ExecutionResult {
            var newState = state

            for ((playerId, count) in revealCounts) {
                repeat(count) {
                    val tokenId = EntityId.generate()
                    val defaultName = "${tokenCreatureTypes.joinToString(" ")} Token"
                    val tokenComponent = CardComponent(
                        cardDefinitionId = "token:$defaultName",
                        name = defaultName,
                        manaCost = ManaCost.ZERO,
                        typeLine = TypeLine.parse("Creature - ${tokenCreatureTypes.joinToString(" ")}"),
                        baseStats = CreatureStats(tokenPower, tokenToughness),
                        baseKeywords = emptySet(),
                        colors = tokenColors,
                        ownerId = playerId
                    )

                    val container = ComponentContainer.of(
                        tokenComponent,
                        TokenComponent,
                        ControllerComponent(playerId),
                        SummoningSicknessComponent
                    )

                    newState = newState.withEntity(tokenId, container)
                    val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
                    newState = newState.addToZone(battlefieldZone, tokenId)
                }
            }

            return ExecutionResult.success(newState)
        }
    }
}

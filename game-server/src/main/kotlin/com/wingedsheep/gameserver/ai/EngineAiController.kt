package com.wingedsheep.gameserver.ai

import com.wingedsheep.engine.ai.AIPlayer
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.gameserver.dto.ClientGameState
import com.wingedsheep.gameserver.protocol.LegalActionInfo
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.sdk.model.EntityId
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(EngineAiController::class.java)

/**
 * AI controller powered by the built-in rules-engine [AIPlayer].
 *
 * Runs entirely locally with no API calls. Uses the engine's ActionProcessor,
 * board evaluator, multi-ply searcher, and combat advisor directly.
 *
 * Requires a [gameStateProvider] to access the real (unmasked) [GameState] from
 * the [com.wingedsheep.gameserver.session.GameSession]. This allows the engine AI
 * to simulate actions and evaluate board states accurately.
 */
class EngineAiController(
    private val cardRegistry: CardRegistry,
    private val playerId: EntityId,
    private val gameStateProvider: () -> GameState?
) : AiController {

    private val aiPlayer = AIPlayer.create(cardRegistry, playerId)

    override fun chooseAction(
        state: ClientGameState,
        legalActions: List<LegalActionInfo>,
        pendingDecision: PendingDecision?,
        recentGameLog: List<String>
    ): ActionResponse {
        val gameState = gameStateProvider()
        if (gameState == null) {
            logger.warn("Engine AI: no game state available, passing priority")
            return ActionResponse.SubmitAction(PassPriority(playerId))
        }

        // Handle pending decisions using the engine AI's responder
        if (pendingDecision != null && pendingDecision.playerId == playerId) {
            val response = aiPlayer.respondToDecision(gameState, pendingDecision)
            logger.info("Engine AI decision: {} → {}", pendingDecision::class.simpleName, response::class.simpleName)
            return ActionResponse.SubmitDecision(playerId, response)
        }

        if (legalActions.isEmpty()) {
            return ActionResponse.SubmitAction(PassPriority(playerId))
        }

        // Single action → just take it
        if (legalActions.size == 1) {
            return ActionResponse.SubmitAction(legalActions.first().action)
        }

        // Use the engine AI to choose the best action from the real game state
        val action = aiPlayer.chooseAction(gameState)
        logger.info("Engine AI chose: {}", action::class.simpleName)
        return ActionResponse.SubmitAction(action)
    }

    override fun decideMulligan(mulliganMessage: ServerMessage.MulliganDecision): Boolean {
        val handSize = mulliganMessage.hand.size
        val mulliganCount = mulliganMessage.mulliganCount

        if (handSize <= 5 || mulliganCount >= 2) return true

        val cards = mulliganMessage.cards
        val landCount = mulliganMessage.hand.count { entityId ->
            cards[entityId]?.typeLine?.contains("Land", ignoreCase = true) == true
        }

        val keep = landCount in 2..5
        logger.info("Engine AI mulligan: hand={} cards, {} lands → {}", handSize, landCount, if (keep) "KEEP" else "MULLIGAN")
        return keep
    }

    override fun chooseBottomCards(message: ServerMessage.ChooseBottomCards): List<EntityId> {
        val count = message.cardsToPutOnBottom
        if (count <= 0 || message.hand.isEmpty()) return emptyList()

        val cards = message.cards

        // Rank cards: keep lands (up to 3) and cheap spells, bottom excess lands and expensive spells
        val lands = mutableListOf<EntityId>()
        val spells = mutableListOf<EntityId>()

        for (entityId in message.hand) {
            val isLand = cards[entityId]?.typeLine?.contains("Land", ignoreCase = true) == true
            if (isLand) lands.add(entityId) else spells.add(entityId)
        }

        val toBottom = mutableListOf<EntityId>()
        val targetLands = if (message.hand.size - count <= 5) 2 else 3

        // Bottom excess lands
        if (lands.size > targetLands) {
            toBottom.addAll(lands.drop(targetLands))
        }

        // If we need more, bottom most expensive spells
        if (toBottom.size < count) {
            val expensive = spells.sortedByDescending { entityId ->
                parseCmc(cards[entityId]?.manaCost ?: "")
            }
            for (spell in expensive) {
                if (toBottom.size >= count) break
                toBottom.add(spell)
            }
        }

        val result = toBottom.take(count)
        logger.info("Engine AI bottom cards: {} of {} → {}", result.size, message.hand.size,
            result.mapNotNull { cards[it]?.name })
        return result
    }

    override fun setDeckList(deckList: Map<String, Int>, archetype: String?) {
        // Engine AI evaluates board state directly — deck knowledge isn't needed
    }

    private fun parseCmc(manaCost: String): Int {
        var cmc = 0
        val regex = Regex("\\{([^}]+)\\}")
        for (match in regex.findAll(manaCost)) {
            val symbol = match.groupValues[1]
            val numericValue = symbol.toIntOrNull()
            if (numericValue != null) cmc += numericValue
            else if (symbol != "X") cmc += 1
        }
        return cmc
    }
}

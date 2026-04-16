package com.wingedsheep.ai.engine

import com.wingedsheep.ai.ActionResponse
import com.wingedsheep.ai.AiPlayerController
import com.wingedsheep.ai.engine.advisor.modules.BloomburrowAdvisorModule
import com.wingedsheep.ai.engine.advisor.modules.OnslaughtAdvisorModule
import com.wingedsheep.ai.llm.BottomCardsInfo
import com.wingedsheep.ai.llm.CardSummary
import com.wingedsheep.ai.llm.MulliganInfo
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.view.ClientGameState
import com.wingedsheep.engine.view.LegalActionInfo
import com.wingedsheep.sdk.model.EntityId
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(EngineAiPlayerController::class.java)

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
class EngineAiPlayerController(
    private val cardRegistry: CardRegistry,
    private val playerId: EntityId,
    private val gameStateProvider: () -> GameState?
) : AiPlayerController {

    private val aiPlayer = AIPlayer.create(
        cardRegistry, playerId,
        advisorModules = listOf(BloomburrowAdvisorModule(), OnslaughtAdvisorModule())
    )

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

        // Single action → just take it, UNLESS it's a combat declaration
        // (DeclareAttackers/DeclareBlockers default to empty maps and need the AI to fill them in)
        if (legalActions.size == 1) {
            val action = legalActions.first().action
            val isCombatDeclaration = action is DeclareAttackers || action is DeclareBlockers
            if (!isCombatDeclaration) {
                return ActionResponse.SubmitAction(action)
            }
        }

        // Use the engine AI to choose the best action from the real game state
        val action = aiPlayer.chooseAction(gameState)
        logger.info("Engine AI chose: {}", action::class.simpleName)
        return ActionResponse.SubmitAction(action)
    }

    override fun decideMulligan(mulliganMessage: MulliganInfo): Boolean {
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

    override fun chooseBottomCards(message: BottomCardsInfo): List<EntityId> {
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

    // =========================================================================
    // Draft Picking (Heuristic)
    // =========================================================================

    /** Colors committed to so far, tracked by counting colored mana symbols in picked cards. */
    private fun inferColors(pickedSoFar: List<CardSummary>): Map<Char, Int> {
        val colorCounts = mutableMapOf<Char, Int>()
        for (card in pickedSoFar) {
            val cost = card.manaCost ?: continue
            for (symbol in listOf('W', 'U', 'B', 'R', 'G')) {
                val count = cost.count { it == symbol }
                if (count > 0) colorCounts[symbol] = (colorCounts[symbol] ?: 0) + count
            }
        }
        return colorCounts
    }

    override fun chooseDraftPick(
        pack: List<CardSummary>,
        pickedSoFar: List<CardSummary>,
        packNumber: Int,
        pickNumber: Int,
        picksRequired: Int,
        passDirection: String
    ): List<String> {
        val colorCommitment = inferColors(pickedSoFar)
        val ranked = pack.sortedByDescending { rateCard(it, colorCommitment, pickedSoFar) }
        val picks = ranked.take(picksRequired).map { it.name }
        logger.info("Engine AI draft pick P{}p{}: {} (from {} cards, committed colors: {})",
            packNumber, pickNumber, picks.joinToString(", "), pack.size,
            colorCommitment.entries.sortedByDescending { it.value }.take(2).joinToString("") { "${it.key}" })
        return picks
    }

    override fun chooseWinstonAction(
        pileCards: List<CardSummary>,
        pileIndex: Int,
        pileSizes: List<Int>,
        pickedSoFar: List<CardSummary>
    ): Boolean {
        val colorCommitment = inferColors(pickedSoFar)
        val totalRating = pileCards.sumOf { rateCard(it, colorCommitment, pickedSoFar) }
        // Take pile if average card quality is decent, or if there's a standout card
        val avgRating = totalRating / pileCards.size.coerceAtLeast(1)
        val bestCard = pileCards.maxByOrNull { rateCard(it, colorCommitment, pickedSoFar) }
        val bestRating = if (bestCard != null) rateCard(bestCard, colorCommitment, pickedSoFar) else 0.0

        // More willing to take larger piles (more cards = more value even if some are weak)
        val sizeBonus = (pileCards.size - 1) * 1.0
        val take = avgRating + sizeBonus >= 5.0 || bestRating >= 8.0

        // On the last pile, always take (forced by Winston rules, but just in case)
        val isLastPile = pileIndex == pileSizes.size - 1

        logger.info("Engine AI Winston pile {}: {} cards, avg={}, best={} → {}",
            pileIndex, pileCards.size, "%.1f".format(avgRating), "%.1f".format(bestRating),
            if (take || isLastPile) "TAKE" else "SKIP")
        return take || isLastPile
    }

    override fun chooseGridDraftPick(
        grid: List<CardSummary?>,
        availableSelections: List<String>,
        pickedSoFar: List<CardSummary>
    ): String {
        val colorCommitment = inferColors(pickedSoFar)

        // Rate each available selection by sum of card ratings in that row/column
        val best = availableSelections.maxByOrNull { selection ->
            val cards = getGridCards(grid, selection)
            cards.sumOf { rateCard(it, colorCommitment, pickedSoFar) }
        } ?: availableSelections.first()

        val cards = getGridCards(grid, best)
        logger.info("Engine AI grid pick: {} (cards: {})",
            best, cards.joinToString(", ") { it.name })
        return best
    }

    private fun getGridCards(grid: List<CardSummary?>, selection: String): List<CardSummary> {
        val indices = when {
            selection.startsWith("ROW_") -> {
                val row = selection.removePrefix("ROW_").toInt()
                listOf(row * 3, row * 3 + 1, row * 3 + 2)
            }
            selection.startsWith("COL_") -> {
                val col = selection.removePrefix("COL_").toInt()
                listOf(col, col + 3, col + 6)
            }
            else -> emptyList()
        }
        return indices.mapNotNull { if (it < grid.size) grid[it] else null }
    }

    /**
     * Rate a card for draft picking. Higher = better pick.
     * Considers rarity, creature stats, removal potential, mana curve, and color fit.
     */
    private fun rateCard(card: CardSummary, colorCommitment: Map<Char, Int>, pickedSoFar: List<CardSummary>): Double {
        var score = 0.0

        // Rarity bonus
        score += when (card.rarity?.uppercase()) {
            "MYTHIC" -> 12.0
            "RARE" -> 10.0
            "UNCOMMON" -> 6.0
            "COMMON" -> 3.0
            else -> 2.0
        }

        // Creature stats bonus
        if (card.power != null && card.toughness != null) {
            val stats = card.power + card.toughness
            val cmc = parseCmc(card.manaCost ?: "")
            // Efficient creatures score higher (stats relative to cost)
            if (cmc > 0) {
                score += (stats.toDouble() / cmc) * 2.0
            }
            score += 1.0 // Creatures are generally good in limited
        }

        // Removal / interaction bonus (heuristic: check oracle text)
        val oracle = card.oracleText?.lowercase() ?: ""
        if (oracle.contains("destroy") || oracle.contains("exile") || oracle.contains("damage") ||
            oracle.contains("fight") || oracle.contains("-") && oracle.contains("/-")) {
            score += 3.0
        }

        // Evasion bonus
        if (oracle.contains("flying") || oracle.contains("menace") || oracle.contains("trample") ||
            oracle.contains("unblockable") || oracle.contains("can't be blocked")) {
            score += 2.0
        }

        // Card advantage bonus
        if (oracle.contains("draw") || oracle.contains("create") && oracle.contains("token")) {
            score += 2.0
        }

        // Color fit — reward cards that match our committed colors, penalize off-color
        val cardColors = extractColors(card.manaCost)
        if (cardColors.isNotEmpty() && colorCommitment.isNotEmpty()) {
            val topColors = colorCommitment.entries.sortedByDescending { it.value }.take(2).map { it.key }.toSet()
            val onColor = cardColors.all { it in topColors }
            val splashable = cardColors.size == 1 && parseCmc(card.manaCost ?: "") >= 4
            when {
                onColor -> score += 2.0
                splashable -> score += 0.5
                pickedSoFar.size >= 8 -> score -= 3.0  // Penalize off-color after early picks
                else -> {} // Early picks: don't penalize too much, still exploring
            }
        }

        // Mana curve consideration — prefer 2-4 drops in limited
        val cmc = parseCmc(card.manaCost ?: "")
        if (cmc in 2..4) score += 1.0
        if (cmc >= 7) score -= 1.0

        // Lands are generally low priority in draft (you get basics)
        if (card.typeLine?.contains("Land", ignoreCase = true) == true) {
            score -= 2.0
            // But nonbasic dual lands are good
            if (oracle.contains("add") && (oracle.contains("or") || oracle.contains("any color"))) {
                score += 4.0
            }
        }

        return score
    }

    private fun extractColors(manaCost: String?): Set<Char> {
        if (manaCost == null) return emptySet()
        return setOf('W', 'U', 'B', 'R', 'G').filter { it in manaCost }.toSet()
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

package com.wingedsheep.ai.engine

import com.wingedsheep.ai.ActionResponse
import com.wingedsheep.ai.AiPlayerController
import com.wingedsheep.ai.draftsim.DraftsimData
import com.wingedsheep.ai.draftsim.DraftsimScorer
import com.wingedsheep.ai.draftsim.ScorerCard
import com.wingedsheep.ai.draftsim.toScorerCard
import com.wingedsheep.ai.draftsim.withFallbackRatings
import com.wingedsheep.ai.draftsim.withSynergyAxes
import com.wingedsheep.ai.insight.AiInsightSink
import com.wingedsheep.ai.llm.BottomCardsInfo
import com.wingedsheep.ai.llm.CardSummary
import com.wingedsheep.ai.llm.MulliganInfo
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.view.ClientGameState
import com.wingedsheep.engine.view.LegalActionInfo
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.model.EntityId
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(EngineAiPlayerController::class.java)

/**
 * AI controller powered by the built-in rules-engine [AIPlayer].
 *
 * Runs entirely locally with no API calls. Uses the engine's ActionProcessor, board evaluator,
 * [Strategist] and [CombatAdvisor] directly, configured by [AiProfile.LIVE] — which is
 * [AiProfile.PRODUCTION_CANDIDATE_EXPIRING] plus whatever fixes have shipped since; read its body
 * for the current list. The candidate it starts from adds rollout candidate evaluation over a
 * determinized state, on a four-tier decision budget, with a land drop priced as the card conversion it is, land *order* priced by the mana it makes usable,
 * a combat trick held until blocks are in and searched properly once they are, the race scored
 * in urgency rather than in turns, removal held for a target worth a card, and a creature priced by
 * what it can still do — marked damage wearing off at cleanup, "can't attack" costing the power —
 * a cantrip cashed in the window where its mana was going to evaporate anyway, a counterspell
 * held while the caster still has the mana for something bigger, and an activated ability whose
 * whole payoff expires at cleanup held for a window that can actually spend it.
 * Each superseded configuration is kept as the baseline its successor was measured against:
 * [AiProfile.PRODUCTION_CANDIDATE_COUNTERPATIENCE] ran immediately before it,
 * [AiProfile.PRODUCTION_CANDIDATE_CANTRIP] before it,
 * [AiProfile.PRODUCTION_CANDIDATE_BOARDVALUE] before it,
 * [AiProfile.PRODUCTION_CANDIDATE_PATIENCE] before it,
 * [AiProfile.PRODUCTION_CANDIDATE_RACECLOCK] before it,
 * [AiProfile.PRODUCTION_CANDIDATE_TRICKWINDOW] before it,
 * [AiProfile.PRODUCTION_CANDIDATE_LANDSEQ] before it,
 * [AiProfile.PRODUCTION_CANDIDATE_LANDDROP] before that,
 * [AiProfile.PRODUCTION_CANDIDATE_TUNED] before that, and the greedy 1-ply
 * [AiProfile.PRODUCTION] first of all.
 *
 * Requires a [gameStateProvider] to access the real (unmasked) [GameState] from
 * the [com.wingedsheep.gameserver.session.GameSession]. This allows the engine AI
 * to simulate actions and evaluate board states accurately.
 */
class EngineAiPlayerController(
    private val cardRegistry: CardRegistry,
    private val playerId: EntityId,
    private val gameStateProvider: () -> GameState?,
    /**
     * Local testing mode: publishes the scores the [Strategist] assigned each candidate, so a human
     * can browse what the AI weighed. Null in normal play.
     */
    insightSink: AiInsightSink? = null,
    /** The profile this seat plays — [AiProfile.LIVE] in production; tests pass variants. */
    private val profile: AiProfile = AiProfile.LIVE,
) : AiPlayerController {

    private val aiPlayer =
        AIPlayer.create(
            cardRegistry, playerId, profile, insightSink = insightSink,
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

        // A combat declaration the server routed to us from another seat (Master Warcraft, Brutal
        // Hordechief): it is still that seat's declaration, validated against that seat's creatures
        // and requirements, so it must be built for that seat — our own enumeration never offers it.
        val routed = legalActions.map { it.action }.firstOrNull {
            (it is DeclareBlockers && it.playerId != playerId) || (it is DeclareAttackers && it.playerId != playerId)
        }
        if (routed != null) {
            val action = declarationFor(routed, gameState)
            logger.info("Engine AI made {}'s routed declaration: {}", routed.playerId, action::class.simpleName)
            return ActionResponse.SubmitAction(action)
        }

        // Use the engine AI to choose the best action from the real game state
        val action = aiPlayer.chooseAction(gameState)
        logger.info("Engine AI chose: {}", action::class.simpleName)
        return ActionResponse.SubmitAction(action)
    }

    private val seatPlayers = mutableMapOf<EntityId, AIPlayer>()

    /**
     * The [offered] declaration's seat owes, chosen by an engine AI playing that seat. It doesn't
     * exploit the choice for us, but it honours the seat's own must-block / must-attack
     * requirements, where a declaration under our own id is rejected outright. Falls back to the
     * offered (empty) declaration if that AI picks anything else.
     */
    private fun declarationFor(offered: GameAction, gameState: GameState): GameAction {
        val seat = offered.playerId
        val chosen = seatPlayers.getOrPut(seat) { AIPlayer.create(cardRegistry, seat, AiProfile.LIVE) }
            .chooseAction(gameState)
        return if (chosen::class == offered::class && chosen.playerId == seat) chosen else offered
    }

    override fun decideMulligan(mulliganMessage: MulliganInfo): Boolean {
        // Momir Basic: every deck is 60 basic lands and the avatar's only cost is generic {X}, so
        // every opening hand is interchangeable — a mulligan can only shrink the hand (the London
        // mulligan bottoms a card each time) without ever improving it. Always keep. Without this,
        // the generic "6-7 lands is a flood → mulligan" heuristic below mulligans every all-lands
        // Momir hand down to the forced keep at mulliganCount >= 2, leaving the AI on a 5-card hand.
        if (gameStateProvider()?.format is Format.MomirBasic) return true

        val handSize = mulliganMessage.hand.size
        val mulliganCount = mulliganMessage.mulliganCount

        if (handSize <= 5 || mulliganCount >= 2) return true

        val cards = mulliganMessage.cards
        val landCount = mulliganMessage.hand.count { entityId ->
            cards[entityId]?.typeLine?.contains("Land", ignoreCase = true) == true
        }

        // A 2–5-lander that can't cast its spells early — wrong colours for all of them, or two
        // lands and nothing to do on them — goes back. Only a seven-card keep is read this way.
        val keep = landCount in 2..5 && !(
            profile.curveAwareMulligan && mulliganMessage.cardsToPutOnBottom == 0 &&
                OpeningHandReader.shouldMulligan(
                    mulliganMessage.hand.mapNotNull { cards[it] }, mulliganMessage.isOnThePlay,
                )
            )
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
                LimitedPickScorer.parseCmc(cards[entityId]?.manaCost ?: "")
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
    // Draft Picking (Draftsim)
    // =========================================================================
    //
    // Picks use the same Draftsim scorer and set tables that build the AI's deck from the drafted
    // pool (DraftsimDeckBuildAdvisor), so the bot drafts toward the deck it will actually build.
    // Cards the set's table leaves unrated — every card of a set we ship no ratings file for — are
    // rated by [LimitedCardRater] off their definitions rather than by rarity alone, and every card
    // carries its structural synergy reading, so a pick feeds the payoffs already taken.

    /** Pick value of each card in [cards] given what the bot has already taken. */
    private fun scoreCards(cards: List<CardSummary>, pickedSoFar: List<CardSummary>): Map<String, Double> {
        val setCodes = (cards + pickedSoFar).mapNotNull { it.setCode?.takeIf(String::isNotBlank) }.distinct()
        val definitions = (cards + pickedSoFar).mapNotNull { cardRegistry.getCard(it.name) }
        val tables = DraftsimData.tablesFor(setCodes).withFallbackRatings(definitions).withSynergyAxes(definitions)
        val scores = DraftsimScorer(tables).scoreBoosterAuto(cards.map(::scorerCard), pickedSoFar.map(::scorerCard))
        return cards.associate { it.name to (scores[it.name]?.total ?: 0.0) }
    }

    /** The full definition where the registry has it (declared colors, color identity), else the summary. */
    private fun scorerCard(card: CardSummary): ScorerCard =
        cardRegistry.getCard(card.name)?.toScorerCard() ?: card.toScorerCard()

    override fun chooseDraftPick(
        pack: List<CardSummary>,
        pickedSoFar: List<CardSummary>,
        packNumber: Int,
        pickNumber: Int,
        picksRequired: Int,
        passDirection: String
    ): List<String> {
        val scores = scoreCards(pack, pickedSoFar)
        val picks = pack.sortedByDescending { scores[it.name] ?: 0.0 }.take(picksRequired).map { it.name }
        logger.info("Engine AI draft pick P{}p{}: {} (from {} cards)", packNumber, pickNumber, picks.joinToString(", "), pack.size)
        return picks
    }

    override fun chooseWinstonAction(
        pileCards: List<CardSummary>,
        pileIndex: Int,
        pileSizes: List<Int>,
        pickedSoFar: List<CardSummary>
    ): Boolean {
        val scores = scoreCards(pileCards, pickedSoFar).values
        val best = scores.maxOrNull() ?: 0.0
        // What the pile adds over the filler a skipped pile still yields: take a standout card, or
        // enough above-filler cards together.
        val surplus = scores.sumOf { (it - WINSTON_FILLER).coerceAtLeast(0.0) }
        val take = best >= WINSTON_STANDOUT || surplus >= WINSTON_PILE_SURPLUS

        // On the last pile, always take (forced by Winston rules, but just in case)
        val isLastPile = pileIndex == pileSizes.size - 1

        logger.info("Engine AI Winston pile {}: {} cards, best={}, surplus={} → {}",
            pileIndex, pileCards.size, "%.1f".format(best), "%.1f".format(surplus),
            if (take || isLastPile) "TAKE" else "SKIP")
        return take || isLastPile
    }

    override fun chooseGridDraftPick(
        grid: List<CardSummary?>,
        availableSelections: List<String>,
        pickedSoFar: List<CardSummary>
    ): String {
        // Rate each available selection by the sum of its cards' pick values
        val scores = scoreCards(grid.filterNotNull(), pickedSoFar)
        val best = availableSelections.maxByOrNull { selection ->
            getGridCards(grid, selection).sumOf { scores[it.name] ?: 0.0 }
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

    private companion object {
        /** A Winston pile holding a card this good (Draftsim pick value) is worth taking for it alone. */
        const val WINSTON_STANDOUT = 4.0
        /** Pick value of replacement-level filler: what a skipped pile still hands you. */
        const val WINSTON_FILLER = 2.0
        /** Summed value above filler at which a pile of lesser cards is worth taking. */
        const val WINSTON_PILE_SURPLUS = 3.0
    }
}

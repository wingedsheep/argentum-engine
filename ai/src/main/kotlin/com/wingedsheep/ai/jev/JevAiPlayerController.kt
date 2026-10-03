package com.wingedsheep.ai.jev

import com.wingedsheep.ai.ActionResponse
import com.wingedsheep.ai.AiPlayerController
import com.wingedsheep.ai.llm.*
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.view.ClientGameState
import com.wingedsheep.engine.view.LegalActionInfo
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

/** Only masked DTOs reach Jev. The validator checks against the authoritative state locally. */
class JevAiPlayerController(
    private val playerId: EntityId,
    private val client: JevChoiceClient,
    private val fallback: AiPlayerController,
    private val validate: (ActionResponse) -> String?,
    private val timeoutMs: Long = 30_000,
    /** Server integrations rebuild this without debug/spectator visibility, even in local mode. */
    private val maskedStateProvider: (() -> ClientGameState)? = null,
) : AiPlayerController {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val json = Json { encodeDefaults = false }
    private var deck = ""

    override fun setDeckList(deckList: Map<String, Int>, archetype: String?) {
        deck = "Deck (order unknown): $deckList. Archetype: ${archetype.orEmpty()}"
        fallback.setDeckList(deckList, archetype)
    }

    override fun chooseAction(state: ClientGameState, legalActions: List<LegalActionInfo>,
                              pendingDecision: PendingDecision?, recentGameLog: List<String>): ActionResponse = guarded(
        { fallback.chooseAction(state, legalActions, pendingDecision, recentGameLog) }
    ) {
        val view = maskedStateProvider?.invoke() ?: state
        require(view.viewingPlayerId == playerId) { "Jev received another player's perspective" }
        require(pendingDecision == null || pendingDecision.playerId == playerId)
        val observation = compact(json.encodeToJsonElement(view)).toString()
        // The incoming debug-mode event log can contain revealed opponent draws. Do not export it.
        val q = session(observation)
        fun label(id: EntityId): String = view.cards[id]?.let { "${it.name} [${id.value}]" }
            ?: view.players.find { it.playerId == id }?.let { "Player ${id.value}" } ?: id.value
        repeat(2) {
            val response = if (pendingDecision != null) {
                q.context("Pending decision: ${compact(json.encodeToJsonElement<PendingDecision>(pendingDecision))}")
                ActionResponse.SubmitDecision(playerId, JevDecisions(q, ::label) { play ->
                    val info = q.pick(play.prompt, legalActions.filter { it.isAffordable }) { it.description }
                    JevActions(q, view, ::label).complete(info)
                }.answer(pendingDecision))
            } else {
                // Auto-pay handles ordinary mana; explicitly offered payment choices remain Jev's.
                val choices = legalActions.filter { it.isAffordable && !it.isManaAbility }
                val info = q.pick("Choose your next play", choices) { it.description }
                q.context("Chosen action requirements: ${compact(json.encodeToJsonElement(info))}")
                ActionResponse.SubmitAction(JevActions(q, view, ::label).complete(info))
            }
            val error = validate(response)
            if (error == null) return@guarded response
            val submitted = when (response) {
                is ActionResponse.SubmitAction -> json.encodeToString<GameAction>(response.action)
                is ActionResponse.SubmitDecision -> json.encodeToString<DecisionResponse>(response.response)
            }
            q.context("Engine rejected $submitted: $error. Correct the move; do not repeat it.")
            logger.info("Jev move rejected by engine; requesting a correction: {}", error)
        }
        error("Jev could not construct a legal move after correction")
    }

    override fun decideMulligan(mulliganMessage: MulliganInfo): Boolean = guarded({ fallback.decideMulligan(mulliganMessage) }) {
        val q = session("Opening hand: ${hand(mulliganMessage.hand, mulliganMessage.cards)}. " +
            "Mulligans so far: ${mulliganMessage.mulliganCount}. On play: ${mulliganMessage.isOnThePlay}. " +
            "Cards to bottom if kept: ${mulliganMessage.cardsToPutOnBottom}.")
        q.pick("Keep this hand or mulligan?", listOf(true, false)) { if (it) "Keep" else "Mulligan" }
    }

    override fun chooseBottomCards(message: BottomCardsInfo): List<EntityId> = guarded({ fallback.chooseBottomCards(message) }) {
        session("Opening hand: ${hand(message.hand, message.cards)}").select("Put cards on bottom (first chosen is topmost)",
            message.hand, message.cardsToPutOnBottom, message.cardsToPutOnBottom) { summary(message.cards[it]) + " [${it.value}]" }
    }

    override fun chooseDraftPick(pack: List<CardSummary>, pickedSoFar: List<CardSummary>, packNumber: Int, pickNumber: Int,
                                 picksRequired: Int, passDirection: String): List<String> = guarded(
        { fallback.chooseDraftPick(pack, pickedSoFar, packNumber, pickNumber, picksRequired, passDirection) }
    ) {
        session("Draft pack $packNumber pick $pickNumber passing $passDirection. Pool: ${pickedSoFar.map(::summary)}")
            .select("Pick cards for your deck", pack.indices.toList(), picksRequired, picksRequired) { summary(pack[it]) }.map { pack[it].name }
    }

    override fun chooseWinstonAction(pileCards: List<CardSummary>, pileIndex: Int, pileSizes: List<Int>, pickedSoFar: List<CardSummary>): Boolean =
        guarded({ fallback.chooseWinstonAction(pileCards, pileIndex, pileSizes, pickedSoFar) }) {
            session("Winston draft: pile $pileIndex of sizes $pileSizes: ${pileCards.map(::summary)}. Pool: ${pickedSoFar.map(::summary)}")
                .pick("Take this pile or skip?", listOf(true, false)) { if (it) "Take pile" else "Skip pile" }
        }

    override fun chooseGridDraftPick(grid: List<CardSummary?>, availableSelections: List<String>, pickedSoFar: List<CardSummary>): String =
        guarded({ fallback.chooseGridDraftPick(grid, availableSelections, pickedSoFar) }) {
            session("Grid draft (row-major, indexed from zero): ${grid.map(::summary)}. Pool: ${pickedSoFar.map(::summary)}")
                .pick("Choose a row or column", availableSelections)
        }

    private fun session(state: String) = JevChoices(client, "$deck\n$state", timeoutMs)
    private fun hand(ids: List<EntityId>, cards: Map<EntityId, CardSummary>) = ids.map { "${it.value}: ${summary(cards[it])}" }
    private fun summary(card: CardSummary?): String = card?.let {
        "${it.name} ${it.manaCost.orEmpty()} ${it.typeLine.orEmpty()} ${it.power ?: ""}/${it.toughness ?: ""} ${it.oracleText.orEmpty()}"
    } ?: "Unknown card"

    private fun <T> guarded(fallbackCall: () -> T, block: () -> T): T = try {
        block()
    } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
        throw e
    } catch (e: Exception) {
        logger.warn("Jev failed ({}); using engine fallback", e.javaClass.simpleName)
        fallbackCall()
    }

    private fun compact(value: JsonElement): JsonElement = when (value) {
        is JsonObject -> JsonObject(value.filterKeys { it !in setOf("imageUri", "imageUrl", "gameLog") }
            .mapValues { compact(it.value) })
        is JsonArray -> JsonArray(value.map(::compact))
        else -> value
    }
}

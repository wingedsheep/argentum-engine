package com.wingedsheep.gameserver.controller

import com.wingedsheep.ai.assist.AdvisorCatalog
import com.wingedsheep.ai.assist.DeckBuildRequest
import com.wingedsheep.ai.assist.DeckBuildResult
import com.wingedsheep.ai.assist.DraftPickAdvice
import com.wingedsheep.ai.assist.DraftPickAdviceRequest
import com.wingedsheep.ai.llm.CardSummary
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gameserver.repository.LobbyRepository
import com.wingedsheep.sdk.model.CardDefinition
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * AI-assistance endpoints: draft "Suggest Pick" and deckbuild "Auto-build".
 *
 * Stateless w.r.t. the draft/deckbuild flow — the client already holds the pack/pool, so it sends
 * card *names* and the server re-resolves them against the [CardRegistry]. The actual brains live
 * in the `ai` module behind a pluggable SPI ([AdvisorCatalog]); this controller only resolves
 * cards, enforces the per-tournament toggle, and dispatches to the engine the player chose.
 *
 * Gating: when a [lobbyId] is supplied and that lobby has `aiAssistEnabled = false`, requests are
 * rejected with 403. Practice/no-lobby requests (null lobbyId) are allowed.
 */
@RestController
@RequestMapping("/api")
class AiAssistController(
    private val cardRegistry: CardRegistry,
    private val lobbyRepository: LobbyRepository,
) {

    // ----- Engine listing (populates the client dropdowns) -----

    data class AdvisorInfo(val id: String, val name: String)
    data class AdvisorsResponse(val draft: List<AdvisorInfo>, val deckbuild: List<AdvisorInfo>)

    @GetMapping("/ai-advisors")
    fun advisors(): AdvisorsResponse = AdvisorsResponse(
        draft = AdvisorCatalog.draftAdvisors.map { AdvisorInfo(it.id, it.displayName) },
        deckbuild = AdvisorCatalog.deckBuildAdvisors.map { AdvisorInfo(it.id, it.displayName) },
    )

    // ----- Draft: suggest a pick -----

    data class SuggestPickBody(
        val lobbyId: String? = null,
        val advisorId: String? = null,
        /** Names of the cards in the current pack (one entry per pickable card). */
        val pack: List<String> = emptyList(),
        /** Names of cards already drafted this event (one entry per copy). */
        val pickedSoFar: List<String> = emptyList(),
        val packNumber: Int = 1,
        val pickNumber: Int = 1,
        val picksRequired: Int = 1,
        /** Set code(s) for set-specific engines; ignored when a known lobby supplies its own. */
        val setCodes: List<String> = emptyList(),
    )

    @PostMapping("/draft/suggest-pick")
    fun suggestPick(@RequestBody body: SuggestPickBody): DraftPickAdvice {
        requireAssistEnabled(body.lobbyId)
        val advisor = AdvisorCatalog.draftAdvisor(body.advisorId)
        val request = DraftPickAdviceRequest(
            pack = body.pack.mapNotNull { cardRegistry.getCard(it)?.toCardSummary() },
            pickedSoFar = body.pickedSoFar.mapNotNull { cardRegistry.getCard(it)?.toCardSummary() },
            packNumber = body.packNumber,
            pickNumber = body.pickNumber,
            picksRequired = body.picksRequired.coerceAtLeast(1),
            setCodes = setCodesFor(body.lobbyId, body.setCodes),
            podSize = lobbyFor(body.lobbyId)?.players?.size,
        )
        return advisor.suggestPick(request)
    }

    // ----- Deckbuild: auto-build / complete -----

    data class AutoBuildBody(
        val lobbyId: String? = null,
        val advisorId: String? = null,
        /** The full card pool as names, one entry per physical copy. */
        val pool: List<String> = emptyList(),
        /** Basic land names available to the build (Plains/Island/…). */
        val basics: List<String> = emptyList(),
        /** Cards already in the deck (name → count). Empty = build fresh; non-empty = complete it. */
        val lockedDeck: Map<String, Int> = emptyMap(),
        val targetSize: Int = 40,
        /** Set code(s) for set-specific engines; ignored when a known lobby supplies its own. */
        val setCodes: List<String> = emptyList(),
    )

    @PostMapping("/deckbuild/auto-build")
    fun autoBuild(@RequestBody body: AutoBuildBody): DeckBuildResult {
        requireAssistEnabled(body.lobbyId)
        val advisor = AdvisorCatalog.deckBuildAdvisor(body.advisorId)
        val request = DeckBuildRequest(
            pool = body.pool.mapNotNull { cardRegistry.getCard(it) },
            availableBasics = body.basics.mapNotNull { cardRegistry.getCard(it) },
            locked = body.lockedDeck.filterValues { it > 0 },
            targetSize = body.targetSize.coerceIn(1, 250),
            setCodes = setCodesFor(body.lobbyId, body.setCodes),
        )
        return advisor.buildDeck(request)
    }

    // ----- Helpers -----

    /** Reject the request when the named lobby has AI assistance turned off. */
    private fun requireAssistEnabled(lobbyId: String?) {
        if (lobbyId.isNullOrBlank()) return
        val lobby = lobbyRepository.findLobbyById(lobbyId) ?: return
        if (!lobby.aiAssistEnabled) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "AI assistance is disabled for this tournament")
        }
    }

    /** Prefer the lobby's authoritative set codes; fall back to the client's list (practice / no lobby). */
    private fun lobbyFor(lobbyId: String?) = lobbyId?.takeIf { it.isNotBlank() }?.let { lobbyRepository.findLobbyById(it) }

    private fun setCodesFor(lobbyId: String?, fromBody: List<String>): List<String> =
        lobbyFor(lobbyId)?.setCodes ?: fromBody

    private fun CardDefinition.toCardSummary() = CardSummary(
        name = name,
        manaCost = if (manaCost.symbols.isEmpty()) null else manaCost.toString(),
        typeLine = typeLine.toString(),
        rarity = metadata.rarity.name,
        imageUri = metadata.imageUri,
        power = creatureStats?.basePower,
        toughness = creatureStats?.baseToughness,
        oracleText = oracleText.ifBlank { null },
    )
}

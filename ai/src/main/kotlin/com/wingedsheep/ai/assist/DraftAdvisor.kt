package com.wingedsheep.ai.assist

import com.wingedsheep.ai.llm.CardSummary

/**
 * A pluggable engine that scores the cards in a draft pack and recommends which to take. The
 * heuristic engine ([HeuristicDraftAdvisor]) ships today; LLM- and MCTS-backed engines can be added
 * later by implementing this interface and registering them in [AdvisorCatalog] — the server
 * endpoint and client UI never change.
 *
 * Operates on [CardSummary] so the same scoring serves both the human "Suggest Pick" button and the
 * AI bot's auto-pick (see [com.wingedsheep.ai.engine.LimitedPickScorer]).
 */
interface DraftAdvisor {
    /** Stable id used by the client to select this engine (e.g. "heuristic"). */
    val id: String

    /** Human-readable name shown in the engine dropdown. */
    val displayName: String

    fun suggestPick(request: DraftPickAdviceRequest): DraftPickAdvice
}

data class DraftPickAdviceRequest(
    /** Every card currently in the pack the player may pick from. */
    val pack: List<CardSummary>,
    /** Cards the player has already drafted this event (drives color commitment). */
    val pickedSoFar: List<CardSummary> = emptyList(),
    val packNumber: Int = 1,
    val pickNumber: Int = 1,
    /** How many cards the player takes this turn (Pick-2 drafts take 2). */
    val picksRequired: Int = 1,
    /**
     * The lobby's set code(s), so set-specific engines (e.g. Draftsim) load the right ratings /
     * removal / archetype tables. Empty for engines that don't need it (the heuristic).
     */
    val setCodes: List<String> = emptyList(),
    /** Drafters at the table, or null when unknown. Drives how much denying a bomb is worth. */
    val podSize: Int? = null,
)

/** A single card's evaluation, surfaced as a badge + tooltip in the draft UI. */
data class CardScore(
    val cardName: String,
    /** 0–100 normalized score for display; higher is a better pick. */
    val score: Int,
    /** Short human justification (e.g. "Removal — premium in limited"). */
    val reason: String,
)

data class DraftPickAdvice(
    val advisorId: String,
    /** One entry per card in the pack, best first. */
    val scores: List<CardScore>,
    /** The [DraftPickAdviceRequest.picksRequired] highest-scoring card names to highlight. */
    val recommended: List<String>,
)

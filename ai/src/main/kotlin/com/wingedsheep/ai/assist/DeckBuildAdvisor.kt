package com.wingedsheep.ai.assist

import com.wingedsheep.sdk.model.CardDefinition

/**
 * A pluggable engine that builds (or completes) a deck from a card pool. The heuristic engine
 * ([HeuristicDeckBuildAdvisor]) ships today; LLM- ([com.wingedsheep.ai.llm.AiDeckBuilder]) and
 * MCTS-backed engines can be added later by implementing this interface and registering them in
 * [AdvisorCatalog].
 *
 * Operates on resolved [CardDefinition]s (the server resolves pool card names against the registry),
 * because building needs the full card — colors, types, mana cost, and the effect tree the heuristic
 * rater walks.
 */
interface DeckBuildAdvisor {
    /** Stable id used by the client to select this engine (e.g. "heuristic"). */
    val id: String

    /** Human-readable name shown in the engine dropdown. */
    val displayName: String

    fun buildDeck(request: DeckBuildRequest): DeckBuildResult
}

data class DeckBuildRequest(
    /** The player's full card pool, one entry per physical copy. */
    val pool: List<CardDefinition>,
    /** Basic lands available to the build, by name (Plains/Island/…). */
    val availableBasics: List<CardDefinition> = emptyList(),
    /**
     * Cards already in the in-progress deck (name → count). Empty = build fresh; non-empty = keep
     * these and only fill the rest ("complete partial").
     */
    val locked: Map<String, Int> = emptyMap(),
    /** Total deck size to reach (40 for limited, higher for commander). */
    val targetSize: Int = 40,
    /**
     * The lobby's set code(s), so set-specific engines (e.g. Draftsim) load the right ratings /
     * removal / archetype tables. Empty for engines that don't need it (the heuristic).
     */
    val setCodes: List<String> = emptyList(),
)

/**
 * The result of a deckbuild: one or more candidate decks the player can choose between. Engines that
 * only produce a single deck (the heuristic, a completion) return a one-element [builds] list;
 * engines that explore several archetypes (Draftsim's "Auto-build") return the best few so the
 * client can show them side by side. [builds] is ordered best-first and never empty on success.
 */
data class DeckBuildResult(
    val advisorId: String,
    /** Candidate decks, ordered best-first. Empty only when the pool yields nothing to build. */
    val builds: List<DeckBuildOption>,
    /** Index into [builds] of the deck to apply by default (the recommended one). */
    val recommended: Int = 0,
)

/** One candidate deck within a [DeckBuildResult]. */
data class DeckBuildOption(
    /** Full decklist (spells + lands, including basics) as name → count. */
    val deckList: Map<String, Int>,
    /** Intrinsic quality estimate (sum of card ratings), or null if the engine doesn't score. */
    val score: Double? = null,
    /** Detected/targeted archetype label, if the engine identifies one. */
    val archetype: String? = null,
    /** The deck's build colors (WUBRG single-letter codes), for the picker's color pips. */
    val colors: List<String> = emptyList(),
)

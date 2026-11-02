package com.wingedsheep.gameserver.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "game")
data class GameProperties(
    val handSmoother: HandSmootherProperties = HandSmootherProperties(),
    val sets: SetsProperties = SetsProperties(),
    val admin: AdminProperties = AdminProperties(),
    val ai: AiProperties = AiProperties(),
    val easterEggs: EasterEggProperties = EasterEggProperties(),
    val tournament: TournamentProperties = TournamentProperties(),
    val debugMode: Boolean = false
)

/**
 * Joke cards that [com.wingedsheep.gameserver.deck.EasterEggDeckInjector] sneaks into a deck based on
 * the player's name. **Off by default** so production stays clean unless someone opts in — the deploy
 * passes no `GAME_EASTER_EGGS_ENABLED`, so a redeploy can't silently switch them back on.
 */
data class EasterEggProperties(
    val enabled: Boolean = false
)

/**
 * Bracket tournament behaviour.
 */
data class TournamentProperties(
    /**
     * Decide an AI-vs-AI bracket match by simulation instead of actually playing it.
     *
     * A round-robin bracket with one human and N AI seats schedules O(N²) matches, and all but N of
     * them are AI against AI — games nobody is in, each one running a full engine game plus two AI
     * controllers on the server. On a shared box that is the bulk of the tournament's CPU, spent on
     * results the human only ever reads off the standings table.
     *
     * On by default; set `GAME_TOURNAMENT_SIMULATE_AI_MATCHES=false` to play every match out (which is
     * what you want on a box where somebody is spectating the AI games). An all-AI bracket — the AI
     * Sandbox, a model-comparison run — is never simulated whatever this says: it was built to be
     * watched, and there would be nothing left of it.
     */
    val simulateAiMatches: Boolean = true,
)

data class HandSmootherProperties(
    val enabled: Boolean = true,
    val candidates: Int = 3
)

/**
 * Set enablement is configured by set code (e.g. "EOE", "DOM").
 *
 * - All sets are enabled by default — every set is selectable in the lobby (not-fully-implemented
 *   ones ride along as "partial" behind the picker's default-off toggle).
 * - Codes in [disabledByDefault] are off unless explicitly enabled in [enabled]. Empty by default;
 *   this is a deliberate admin kill-switch for a set that must be hidden entirely, not a way to
 *   gate work-in-progress sets (the partial-sets toggle handles those).
 * - Codes in [enabled] override [disabledByDefault].
 *
 * Example application.yml:
 * ```
 * game:
 *   sets:
 *     enabled:
 *       SOMESET: false
 * ```
 */
data class SetsProperties(
    val disabledByDefault: Set<String> = emptySet(),
    val enabled: Map<String, Boolean> = emptyMap(),
) {
    fun isEnabled(setCode: String): Boolean {
        val key = setCode.uppercase()
        enabled[key]?.let { return it }
        enabled[setCode]?.let { return it }
        return disabledByDefault.none { it.equals(setCode, ignoreCase = true) }
    }
}

data class AdminProperties(
    val password: String = ""
)

data class AiProperties(
    val enabled: Boolean = false,
    /** AI mode: a built-in mode (`engine` or `llm`) or a registered external provider mode. */
    val mode: String = "engine",
    val baseUrl: String = "https://openrouter.ai/api/v1",
    val apiKey: String = "",
    val openRouterApiKey: String = "",
    val model: String = "qwen/qwen3.6-plus:free",
    val deckbuildingModel: String = "",
    val reasoningEffort: String = "low",
    val maxRetries: Int = 2,
    val timeoutMs: Long = 300000,
    val thinkingDelayMs: Long = 500,
    /**
     * When true, AI limited decks never ask the LLM: they are always built by the Draftsim
     * autobuilder, regardless of per-request flags. Useful for fully local play vs AI.
     */
    val skipLlmDeckbuilding: Boolean = false,
    /**
     * Local testing mode: record what the engine AI considered on every decision and expose it over
     * `/api/dev/ai-insight`, so a human can browse the AI's options with the scores it gave them and
     * export a position plus its ratings as AI-training input.
     *
     * Off by default and deliberately separate from `game.dev-endpoints.enabled`: recording pins a
     * `GameState` per decision, and that is opt-in even on a box where the other dev endpoints are
     * already open. `application-local.yml` turns it on.
     */
    val insightEnabled: Boolean = false
) {
    /** Returns the model to use for deckbuilding — falls back to the gameplay model if not set. */
    val effectiveDeckbuildingModel: String get() = deckbuildingModel.ifBlank { model }

    /** Returns the effective API key — prefers [apiKey], falls back to [openRouterApiKey] for backward compatibility. */
    val effectiveApiKey: String get() = apiKey.ifBlank { openRouterApiKey }

    /** Whether we're using the built-in engine AI (no API key required). */
    val isEngineMode: Boolean get() = mode.equals("engine", ignoreCase = true)

    /** Whether we're using the built-in LLM-based AI. */
    val isLlmMode: Boolean get() = mode.equals("llm", ignoreCase = true)
}

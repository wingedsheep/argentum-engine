package com.wingedsheep.gameserver.scenario

import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.state.components.identity.CommanderRegistryComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step

/**
 * Request/response DTOs and the [ScenarioBuilderService] logic are shared between the
 * dev-only [com.wingedsheep.gameserver.controller.DevScenarioController] and the
 * production [com.wingedsheep.gameserver.controller.ScenarioController]. The shape is the
 * canonical "scenario JSON" that the Scenario Builder UI both produces and pastes.
 */

/** How the opponent seat is filled when a scenario starts. */
enum class ScenarioMode {
    /** Single client controls both seats (hotseat / play against yourself). One token returned. */
    SELF,

    /** One seat is the built-in engine AI ([ScenarioRequest.aiPlayer] selects which). Human token returned. */
    AI,

    /** Two human seats; both tokens returned for hand-off / two-tab play. */
    TWO_PLAYER
}

/** One seat of an N-player scenario ([ScenarioRequest.players]). */
data class ScenarioSeat(
    val name: String? = null,
    val config: PlayerConfig? = null,
)

data class ScenarioRequest(
    val player1Name: String? = "Player1",
    val player2Name: String? = "Player2",
    val player1: PlayerConfig? = null,
    val player2: PlayerConfig? = null,
    /**
     * N-player seats (3-4 player Free-for-All pods), in turn order. When set, it
     * overrides the legacy `player1*`/`player2*` fields. Pods of more than two seats
     * support [ScenarioMode.SELF] (single-client hotseat) only — AI and TWO_PLAYER
     * stay two-seat.
     */
    val players: List<ScenarioSeat>? = null,
    /**
     * Team assignment for team variants (Two-Headed Giant — CR 810; Team vs. Team — CR 808). Each
     * entry is one team: a list of seat indices into [players] (0-based, turn order). When supplied
     * the seats are stamped into teams; [teamVsTeam] selects which team format runs (2HG shares
     * life/turns/combat, Team vs. Team shares nothing). Either team variant is bootable as a hotseat
     * for manual testing. Null = each player plays alone (unchanged). The indices must partition
     * every seat exactly once. Use with [ScenarioMode.SELF] (single-client hotseat).
     */
    val teams: List<List<Int>>? = null,
    /**
     * When [teams] is supplied, run the pod as Team vs. Team (CR 808 — per-player life/turns,
     * individual elimination) instead of the default Two-Headed Giant (CR 810 — shared life/turns).
     * Ignored when [teams] is null. Nullable (like [ScenarioSeat.tapped]) because an absent JSON
     * field maps to null, which `FAIL_ON_NULL_FOR_PRIMITIVES` would reject for a bare `Boolean`.
     */
    val teamVsTeam: Boolean? = false,
    val phase: Phase? = null,
    val step: Step? = null,
    val activePlayer: Int? = null,
    val priorityPlayer: Int? = null,
    /** Steps where player 1 should stop on their own turn (prevents auto-pass) */
    val player1StopAtSteps: List<Step>? = null,
    /** Steps where player 2 should stop on their own turn (prevents auto-pass) */
    val player2StopAtSteps: List<Step>? = null,
    /** Steps where player 1 should stop on opponent's turn (prevents auto-pass) */
    val player1OpponentStopAtSteps: List<Step>? = null,
    /** Steps where player 2 should stop on opponent's turn (prevents auto-pass) */
    val player2OpponentStopAtSteps: List<Step>? = null,
    /**
     * Selects how the opponent seat is filled. When null it is derived for backwards
     * compatibility: [aiPlayer] set ⇒ [ScenarioMode.AI], otherwise [ScenarioMode.TWO_PLAYER].
     */
    val mode: ScenarioMode? = null,
    /**
     * If set to 1 or 2, that seat is played by the built-in engine AI opponent (driven by
     * [com.wingedsheep.gameserver.ai.AiGameManager]). Requires `game.ai.enabled=true`.
     * The other seat is connected normally over WebSocket with the returned token. Scenarios
     * always use the in-process engine AI — no LLM, no API key.
     */
    val aiPlayer: Int? = null
) {
    /** The effective mode, deriving from [aiPlayer] when [mode] is unset. */
    val effectiveMode: ScenarioMode
        get() = mode ?: if (aiPlayer != null) ScenarioMode.AI else ScenarioMode.TWO_PLAYER

    /**
     * The effective seat list, name + config per seat in turn order. The legacy
     * two-seat fields map onto a two-entry list when [players] is unset.
     */
    fun seats(): List<Pair<String, PlayerConfig?>> =
        players?.mapIndexed { i, seat -> (seat.name ?: "Player${i + 1}") to seat.config }
            ?: listOf(
                (player1Name ?: "Player1") to player1,
                (player2Name ?: "Player2") to player2,
            )
}

data class PlayerConfig(
    val lifeTotal: Int? = null,
    val hand: List<String>? = null,
    val battlefield: List<BattlefieldCardConfig>? = null,
    val graveyard: List<String>? = null,
    val library: List<String>? = null,
    val exile: List<String>? = null,
    /**
     * Commander card names. Each name becomes a card in the player's command zone with
     * [CommanderComponent] attached and registered in [CommanderRegistryComponent].
     * Provide one name for a standard commander, two for Partner / Background.
     */
    val commanders: List<String>? = null
)

/**
 * Configuration for a card on the battlefield.
 * For simple cases, just provide the name.
 * For detailed setup, also specify tapped/summoningSickness state.
 * For auras, set [attachedTo] to the name of the host creature.
 * For counters, provide a map of counter type name to count (e.g., {"PLUS_ONE_PLUS_ONE": 3}).
 */
data class BattlefieldCardConfig(
    val name: String,
    val tapped: Boolean? = false,
    val summoningSickness: Boolean? = false,
    val counters: Map<String, Int>? = null,
    val attachedTo: String? = null,
    /** For permanents with "As this enters, choose a creature type" — skips the ETB choice by pre-setting it. */
    val chosenCreatureType: String? = null,
    /**
     * For permanents with "As this enters, choose a color" — skips the ETB choice by pre-setting it.
     * Value must be a [com.wingedsheep.sdk.core.Color] name, e.g. "WHITE", "GREEN".
     */
    val chosenColor: String? = null
)

data class ScenarioResponse(
    val sessionId: String,
    val player1: PlayerInfo,
    val player2: PlayerInfo,
    val message: String,
    /** Echoes the resolved mode so the client knows whether it controls one seat or both. */
    val mode: ScenarioMode? = null,
    /**
     * Full seat roster in turn order (N-player pods). Present whenever the scenario has
     * more than two seats; `player1`/`player2` keep mirroring the first two for the
     * existing 2-player tooling.
     */
    val players: List<PlayerInfo>? = null
)

data class PlayerInfo(
    val name: String,
    val token: String,
    val playerId: String
)

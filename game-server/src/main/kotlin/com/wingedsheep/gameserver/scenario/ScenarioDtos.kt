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
    /**
     * Free-for-All attack rule for an N-player pod (CR 802 / 803): `LEFT`, `RIGHT` or `MULTIPLE`.
     * Null keeps the engine default (`MULTIPLE`), so existing scenarios are unchanged.
     */
    val attackMode: com.wingedsheep.sdk.core.AttackMode? = null,
    /** Play the scenario for ante (CR 407) — shows the ante zones and lets ante cards work. */
    val playingForAnte: Boolean? = false,
    /**
     * Run the scenario as a Commander game (`Format.Commander`): 40 starting life for every seat
     * that sets no [PlayerConfig.lifeTotal], and commander damage tallied against the 21 threshold.
     * Pair it with [PlayerConfig.commanders]. Nullable like [teamVsTeam]: an absent field is null.
     */
    val commanderFormat: Boolean? = false,
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
    val aiPlayer: Int? = null,
    /**
     * Custom cards for this scenario, each a Scryfall(-style) card object as pasted. Compiled by
     * Argentum Assay into real `CardDefinition`s and registered into a **session-scoped** overlay
     * registry, so the names below can be used in any zone exactly like corpus cards while never
     * touching the live corpus. Dev-gated; see [AssayCardService].
     *
     * A card whose Oracle text Assay cannot read *whole* is rejected with the line that stopped it —
     * a partially-read card would test green while missing an ability.
     */
    val customCards: List<String>? = null,
    /** 1-based seat that starts as the monarch (CR 725), or null for no monarch. */
    val monarch: Int? = null,
    /** 1-based seat that starts with the initiative (CR 726), or null for nobody. Staged, so nothing triggers. */
    val initiative: Int? = null,
    /** The game's day/night designation (CR 731), or null for neither. Staged: nothing transforms until state-based actions are next checked. */
    val dayNight: com.wingedsheep.sdk.core.DayNight? = null,
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
    /** Staged player counters, e.g. RAD, ENERGY or EXPERIENCE. */
    val counters: Map<String, Int>? = null,
    val hand: List<String>? = null,
    val battlefield: List<BattlefieldCardConfig>? = null,
    val graveyard: List<String>? = null,
    val library: List<String>? = null,
    val exile: List<String>? = null,
    /**
     * Cards "outside the game" (CR 400.11) — the player's sideboard. Needed by any card that
     * reaches outside the game: the wish cycle, and Strixhaven's **Learn** (CR 701.48), whose
     * Lesson half is simply unreachable without one.
     */
    val sideboard: List<String>? = null,
    /** Cards this player owns in the ante (CR 407). Implies the game is played for ante. */
    val ante: List<String>? = null,
    /**
     * Commander card names. Each name becomes a card in the player's command zone with
     * [CommanderComponent] attached and registered in [CommanderRegistryComponent].
     * Provide one name for a standard commander, two for Partner / Background.
     */
    val commanders: List<String>? = null,
    /**
     * A revealed companion (CR 702.139): a card placed in the sideboard and marked as the player's
     * companion, waiting outside the game for the {3} special action.
     */
    val companion: String? = null,
    /**
     * Start this player in a dungeon (CR 309): the dungeon card in their command zone with the
     * venture marker on [DungeonConfig.room]. Nothing triggers — the board is staged as if the
     * player had already ventured there.
     */
    val dungeon: DungeonConfig? = null,
    /** Dungeons this player has already completed (CR 309.7), by name. */
    val completedDungeons: List<String>? = null,
    /**
     * Spells this player has cast this turn — read by the untap step's day/night check (CR 731.2)
     * when the turn passes, so a staged turn can make it day or night next turn.
     */
    val spellsCastThisTurn: Int? = null
)

/** A staged dungeon: its name and the room (by name; the topmost room when omitted) the marker is on. */
data class DungeonConfig(
    val name: String,
    val room: String? = null
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
     * For permanents that durably choose a card type (CR 205.2a — e.g. Arachne, Psionic Weaver's
     * "choose a card type other than creature") — skips the ETB choice by pre-setting it. Value is a
     * card-type name, e.g. "Artifact", "Enchantment", "Instant".
     */
    val chosenCardType: String? = null,
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

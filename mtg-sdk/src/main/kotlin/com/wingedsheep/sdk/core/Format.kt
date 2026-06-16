package com.wingedsheep.sdk.core

import kotlinx.serialization.Serializable

/**
 * Game-mode configuration the engine reads at runtime.
 *
 * Distinct from [DeckFormat], which is a deck-construction concept (Scryfall-sourced legality +
 * singleton/size rules enforced by the deck validator). [Format] tells the engine how to *run*
 * the game: starting life, hand size, win conditions, and any zone setup that depends on format.
 *
 * Adding a new format becomes a config variant rather than a new code path: Brawl, Oathbreaker,
 * Pauper Commander, and 1v1 Commander are all (or will be) `Commander`-shaped data with different
 * field values.
 */
@Serializable
sealed interface Format {

    /**
     * CR 810.4 / 810.9 — the players on a team share **one life total** (and one pooled poison
     * total, CR 810.10). True only for [TwoHeadedGiant]. Every other format — including the
     * [TeamVsTeam] variant (CR 808.5: "a team's resources … are not shared") and all non-team
     * formats — keeps a per-player life total, so this stays false.
     *
     * The engine resolves every life read/write through
     * [com.wingedsheep.engine.state.GameState.teamLifeOwnerOf], which collapses to the player itself
     * whenever this is false.
     */
    val sharesTeamLife: Boolean get() = false

    /**
     * CR 805 — the team takes **one shared turn**: its members untap, draw, attack and block as a
     * single side (CR 805.4 / 805.10), and either may take sorcery-speed actions while it is the
     * team's turn (CR 805.5a). True for [TwoHeadedGiant] (and, in future, the Archenemy variant,
     * CR 904.5). [TeamVsTeam] players take **individual** turns in seat order (CR 808.4), so this is
     * false — each player only untaps/draws/attacks/blocks on their own turn.
     */
    val sharesTeamTurns: Boolean get() = false

    /**
     * CR 810.8a — players win and lose the game **only as a team**: if one member loses, the whole
     * team loses. True for [TwoHeadedGiant]. Under the normal multiplayer rules a player is
     * eliminated individually (CR 104.3b) and a team persists until all its members have left
     * (CR 104.2c), so [TeamVsTeam] and Free-for-All leave this false.
     */
    val playersWinLoseAsTeam: Boolean get() = false

    @Serializable
    data object Standard : Format

    /**
     * 1v1 Commander (Phase 1). Multiplayer (3-4 free-for-all) is its own project.
     *
     * @property commanderDamageThreshold Cumulative single-source combat damage that loses the
     *   game (CR 903.10a). Standard Commander is 21.
     * @property deckSize Total deck size including the commander; the validator enforces this.
     * @property startingLife Each player's starting life total.
     * @property startingHandSize Cards drawn for the opening hand.
     * @property alwaysDivertToCommand Bypass the CR 903.9a player choice and unconditionally
     *   divert a commander leaving for graveyard / exile / hand / library to the command zone.
     *   Off by default — the SBA pauses with a yes/no decision so the owner can choose to leave
     *   the commander in the destination zone (e.g. to keep recursion targets in graveyard or
     *   keep linked-exile abilities tracking it). AI/headless tooling can flip this on to skip
     *   the prompt.
     */
    @Serializable
    data class Commander(
        val commanderDamageThreshold: Int = 21,
        val deckSize: Int = 100,
        val startingLife: Int = 40,
        val startingHandSize: Int = 7,
        val alwaysDivertToCommand: Boolean = false,
    ) : Format

    /**
     * Momir Basic — the classic Vanguard format (<https://mtg.fandom.com/wiki/Momir>).
     *
     * Each player's deck is 60 basic lands and every player begins the game with the avatar
     * [avatarCardName] in the command zone. The avatar grants the activated ability
     * "{X}, Discard a card: Create a token that's a copy of a randomly chosen creature card
     * with mana value X. Activate only as a sorcery and only once each turn."
     *
     * Like [Commander], this is runtime config, not a code path: the engine reads it at game
     * init to set life / hand size and to place the avatar in the command zone, and the
     * random-creature-token effect reads [eligibleCreatureNames] as its candidate pool.
     *
     * @property startingLife 24 — Vanguard's base 20 plus Momir Vig's printed Life Modifier of +4.
     * @property eligibleCreatureNames The pool the random copy is drawn from — every creature card
     *   name across all sets. **Stored pre-sorted** so the engine's seeded `GameRng.pick` is
     *   replay-stable: the effect filters this list by mana value and picks, never re-collecting
     *   from the card registry (whose map order is unspecified). The engine treats it as opaque,
     *   deterministic data; the server computes it at match start.
     */
    @Serializable
    data class MomirBasic(
        val startingLife: Int = 24,
        val startingHandSize: Int = 7,
        val avatarCardName: String = "Momir Vig, Simic Visionary",
        val eligibleCreatureNames: List<String> = emptyList(),
    ) : Format

    /**
     * Two-Headed Giant — the 2v2 team variant (CR 810). Two teams of two players each; a team
     * shares one life total and one pool of poison counters, takes a shared turn (CR 805), fights
     * combined combat, and wins or loses together (CR 810.8a).
     *
     * Like [Commander] this is runtime config, not a code path: the engine reads it at game init
     * to set the shared starting life and (via [com.wingedsheep.engine.core.GameConfig.teams]) to
     * stamp team membership on the player entities. The shared behaviours it enables —
     * [sharesTeamLife] (CR 810.4 / 810.9), [sharesTeamTurns] (CR 805), and [playersWinLoseAsTeam]
     * (CR 810.8a) — are all gated on these capability flags, so the contrast with [TeamVsTeam]
     * (every flag false) is data, not a code path.
     *
     * @property startingLife The team's shared starting life total. Regular two-player 2HG is 30
     *   (CR 810.4); every team member resolves life through the team's canonical owner.
     * @property startingHandSize Cards drawn for each player's opening hand.
     * @property poisonThreshold Poison counters at which a team loses the game (CR 810.8d). 2HG
     *   raises the normal 10 to 15.
     */
    @Serializable
    data class TwoHeadedGiant(
        val startingLife: Int = 30,
        val startingHandSize: Int = 7,
        val poisonThreshold: Int = 15,
    ) : Format {
        override val sharesTeamLife: Boolean get() = true
        override val sharesTeamTurns: Boolean get() = true
        override val playersWinLoseAsTeam: Boolean get() = true
    }

    /**
     * Team vs. Team — the general N-per-team multiplayer variant (CR 808). Two or more teams of any
     * size (2v2, 3v3, 4v4, …) where, unlike [TwoHeadedGiant], **nothing is shared** (CR 808.5):
     * each player keeps their own life total, takes their own turn in seat order (CR 808.4), draws
     * and untaps alone, attacks and blocks only on their own behalf, and is eliminated individually
     * when their own life hits 0 (CR 104.3b). A team loses only once *all* of its players have left
     * the game (CR 104.2c) — the last team with a player still in wins.
     *
     * It reuses the team **membership** model only: opponents exclude teammates (CR 808 / 802.2),
     * the win check is by team (last team standing), and teammates are seated adjacently (CR 808.2).
     * Every shared-team capability flag — [sharesTeamLife], [sharesTeamTurns],
     * [playersWinLoseAsTeam] — stays at its `false` default, which is the entire behavioural
     * difference from 2HG.
     *
     * Like the other formats this is runtime config, not a code path: the engine reads [startingLife]
     * at game init and stamps team membership from [com.wingedsheep.engine.core.GameConfig.teams].
     *
     * @property startingLife Each player's own starting life total (standard 20).
     * @property startingHandSize Cards drawn for each player's opening hand.
     */
    @Serializable
    data class TeamVsTeam(
        val startingLife: Int = 20,
        val startingHandSize: Int = 7,
    ) : Format
}

/**
 * Preset shapes for drafted/sealed 1v1 commander formats. Each preset selects a
 * [Format.Commander] configuration tuned for 60-card limited play (paper Brawl life vs. classic
 * Commander life). The lobby host picks one of these when creating a Commander Draft or Sealed
 * lobby; the match builder converts it to a [Format.Commander] instance at game start.
 */
@Serializable
enum class CommanderPreset(
    val deckSize: Int,
    val startingLife: Int,
    val commanderDamage: Int,
) {
    /** Paper Brawl life total, faster 1v1 games. */
    BRAWL(deckSize = 60, startingLife = 25, commanderDamage = 16),

    /** Closer to Commander Legends' template — slower, more recursive games. */
    COMMANDER(deckSize = 60, startingLife = 30, commanderDamage = 21);

    fun toFormat(): Format.Commander = Format.Commander(
        commanderDamageThreshold = commanderDamage,
        deckSize = deckSize,
        startingLife = startingLife,
    )
}

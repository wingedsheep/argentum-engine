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
     * @property alwaysDivertToCommand Phase 1 short-cut for the zone-change replacement: when a
     *   commander would move to graveyard / exile / hand / library, divert to the command zone
     *   automatically. Phase 1.5 will replace this with a player-choice yes/no decision.
     */
    @Serializable
    data class Commander(
        val commanderDamageThreshold: Int = 21,
        val deckSize: Int = 100,
        val startingLife: Int = 40,
        val startingHandSize: Int = 7,
        val alwaysDivertToCommand: Boolean = true,
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

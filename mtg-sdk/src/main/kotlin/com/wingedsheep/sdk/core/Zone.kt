package com.wingedsheep.sdk.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
/**
 * @property displayName the zone as a standalone phrase, article included ("a library", "the
 *   battlefield", "exile") — for "in ${'$'}displayName", "from ${'$'}displayName".
 * @property noun the bare zone noun ("library", "battlefield") — for text where a possessive
 *   precedes the zone ("your library", "its owner's graveyard"), which must never read "your a library".
 */
enum class Zone(val displayName: String, val noun: String) {
    @SerialName("Library") LIBRARY("a library", "library"),
    @SerialName("Hand") HAND("a hand", "hand"),
    @SerialName("Battlefield") BATTLEFIELD("the battlefield", "battlefield"),
    @SerialName("Graveyard") GRAVEYARD("a graveyard", "graveyard"),
    @SerialName("Stack") STACK("the stack", "stack"),
    @SerialName("Exile") EXILE("exile", "exile"),
    @SerialName("Command") COMMAND("the command zone", "command zone"),

    /**
     * A player's sideboard — the cards they own *outside the game* (CR 100.4, 400.11a). Strictly,
     * "outside the game is not a zone" (CR 400.11), but the engine models the sideboard as a
     * private per-player pseudo-zone so the "wish" mechanic (Burning Wish, Living Wish, …) can
     * reach it with the ordinary Gather → Select → Move pipeline instead of bespoke machinery.
     *
     * It is private to its owner (masked from opponents, like [LIBRARY]/[HAND]) and, consistent
     * with CR 400.11c ("cards outside the game can't be affected by spells or abilities"), nothing
     * but a wish effect should ever gather from it. In constructed it is an explicit list (≤15,
     * CR 100.4a); in Limited it is every card in the player's pool not in their deck (CR 100.4b).
     */
    @SerialName("Sideboard") SIDEBOARD("a sideboard", "sideboard");

    val isPublic: Boolean
        get() = this in listOf(BATTLEFIELD, GRAVEYARD, STACK, EXILE, COMMAND)

    val isHidden: Boolean
        get() = this in listOf(LIBRARY, HAND, SIDEBOARD)

    val isShared: Boolean
        get() = this in listOf(BATTLEFIELD, STACK, EXILE)
}

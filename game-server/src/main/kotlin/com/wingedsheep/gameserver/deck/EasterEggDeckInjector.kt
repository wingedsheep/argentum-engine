package com.wingedsheep.gameserver.deck

import org.slf4j.LoggerFactory

/**
 * Injects easter egg cards into player decks based on player name and deck contents.
 */
object EasterEggDeckInjector {

    private val logger = LoggerFactory.getLogger(EasterEggDeckInjector::class.java)

    private const val SEKSHAAS_CARD_NAME = "Sekshaas, Early Sleeper"

    /**
     * If the player is named "Rick" (case-insensitive) and their deck contains
     * both Forest and Plains, inject Sekshaas, Early Sleeper into the deck.
     *
     * A no-op unless [enabled] (`game.easter-eggs.enabled`, off by default) — production runs without
     * the eggs; local dev opts in via `GAME_EASTER_EGGS_ENABLED=true`.
     */
    fun maybeInjectEasterEggs(playerName: String, deck: Map<String, Int>, enabled: Boolean): Map<String, Int> {
        if (!enabled) return deck
        if (!playerName.equals("Rick", ignoreCase = true)) return deck
        if (!deck.containsCard("Forest") || !deck.containsCard("Plains")) return deck

        logger.info("🐇 Rick has Forest and Plains — sneaking Sekshaas, Early Sleeper into deck!")
        return deck + (SEKSHAAS_CARD_NAME to 1)
    }

    /**
     * Deck-list keys carry an optional `#SetCode-CollectorNumber` printing suffix once
     * `BoosterGenerator.withBasicLandArt` has resolved a lobby's basics to a specific art, so match
     * on the card name alone rather than the raw key.
     */
    private fun Map<String, Int>.containsCard(cardName: String): Boolean =
        keys.any { it.substringBefore('#') == cardName }
}

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
     */
    fun maybeInjectEasterEggs(playerName: String, deck: Map<String, Int>): Map<String, Int> {
        if (!playerName.equals("Rick", ignoreCase = true)) return deck
        if (!deck.containsKey("Forest") || !deck.containsKey("Plains")) return deck

        logger.info("🐇 Rick has Forest and Plains — sneaking Sekshaas, Early Sleeper into deck!")
        return deck + (SEKSHAAS_CARD_NAME to 1)
    }
}

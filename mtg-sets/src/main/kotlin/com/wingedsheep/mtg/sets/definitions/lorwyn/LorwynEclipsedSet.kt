package com.wingedsheep.mtg.sets.definitions.lorwyn

import com.wingedsheep.mtg.sets.definitions.lorwyn.cards.*

/**
 * Lorwyn Eclipsed Set (2025)
 *
 * A supplemental set expanding on the Lorwyn plane with new cards
 * featuring the tribes and themes of the original Lorwyn block.
 *
 * Set Code: ECL
 * Card Count: (in progress)
 */
object LorwynEclipsedSet {

    const val SET_CODE = "ECL"
    const val SET_NAME = "Lorwyn Eclipsed"

    /**
     * All cards in this set.
     */
    val allCards = listOf(
        ChangelingWayfinder,
        RooftopPercher,
        AdeptWatershaper,
        AjaniOutlandChaperone,
        AppealToEirdu
    )

    /**
     * Get a card by name.
     */
    fun getCard(name: String) = allCards.find { it.name == name }

    /**
     * Get all cards with a given name.
     */
    fun getCardsByName(name: String) = allCards.filter { it.name == name }

    /**
     * Get a card by collector number.
     */
    fun getCardByNumber(collectorNumber: String) =
        allCards.find { it.metadata.collectorNumber == collectorNumber }

    /**
     * Basic lands for this set.
     * Contains 3 art variants of each basic land type (15 lands total).
     */
    val basicLands = LorwynEclipsedBasicLands
}

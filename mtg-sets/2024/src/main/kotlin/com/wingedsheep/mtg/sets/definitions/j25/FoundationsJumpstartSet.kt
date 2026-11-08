package com.wingedsheep.mtg.sets.definitions.j25

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Foundations Jumpstart (2024)
 *
 * Jumpstart half-deck boosters released alongside Foundations. Played as Jump In: pick two of the
 * 121 published 20-card packs (`game-server/src/main/resources/jumpstart/j25.txt`), like J22.
 *
 * Set Code: J25
 * Release Date: November 15, 2024
 */
object FoundationsJumpstartSet : MtgSet {

    override val code = "J25"
    override val displayName = "Foundations Jumpstart"
    override val releaseDate = "2024-11-15"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.j25.cards"
}

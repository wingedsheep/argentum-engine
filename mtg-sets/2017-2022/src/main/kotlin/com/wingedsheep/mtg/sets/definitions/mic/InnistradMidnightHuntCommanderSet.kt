package com.wingedsheep.mtg.sets.definitions.mic

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Innistrad: Midnight Hunt Commander (2021)
 *
 * Commander preconstructed decks released alongside Innistrad: Midnight Hunt. Scaffolded to hold the
 * canonical [CardDefinition] for cards that first appeared here (e.g. Wilhelt, the Rotcleaver and
 * the other new cards of the Undead Unleashed and Coven Counters decks).
 *
 * Set Code: MIC
 * Release Date: September 24, 2021
 */
object InnistradMidnightHuntCommanderSet : MtgSet {

    override val code = "MIC"
    override val displayName = "Innistrad: Midnight Hunt Commander"
    override val releaseDate = "2021-09-24"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.mic.cards"
}

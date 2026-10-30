package com.wingedsheep.mtg.sets.definitions.brc

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * The Brothers' War Commander (2022)
 *
 * Commander preconstructed decks released alongside The Brothers' War (Urza's Iron Alliance and
 * Mishra's Burnished Banner). Scaffolded to hold the canonical [CardDefinition] for cards that
 * first appeared here (e.g. Scholar of New Horizons).
 *
 * Set Code: BRC
 * Release Date: November 18, 2022
 */
object TheBrothersWarCommanderSet : MtgSet {

    override val code = "BRC"
    override val displayName = "The Brothers' War Commander"
    override val releaseDate = "2022-11-18"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.brc.cards"
}

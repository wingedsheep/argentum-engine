package com.wingedsheep.mtg.sets.definitions.dft

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Aetherdrift (2025)
 *
 * Set Code: DFT
 * Release Date: February 14, 2025
 */
object AetherdriftSet : MtgSet {

    override val code = "DFT"
    override val displayName = "Aetherdrift"
    override val releaseDate = "2025-02-14"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.dft.cards"
}

package com.wingedsheep.mtg.sets.definitions.lci

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * The Lost Caverns of Ixalan Set (2023)
 *
 * Set Code: LCI
 * Release Date: November 17, 2023
 */
object LostCavernsOfIxalanSet : MtgSet {

    override val code = "LCI"
    override val displayName = "The Lost Caverns of Ixalan"
    override val releaseDate = "2023-11-17"
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.lci.cards"
}

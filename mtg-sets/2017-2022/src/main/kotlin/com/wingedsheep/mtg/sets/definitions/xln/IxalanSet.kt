package com.wingedsheep.mtg.sets.definitions.xln

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Ixalan (2017)
 *
 * Set Code: XLN
 * Release Date: September 29, 2017
 */
object IxalanSet : MtgSet {

    override val code = "XLN"
    override val displayName = "Ixalan"
    override val releaseDate = "2017-09-29"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.xln.cards"
}

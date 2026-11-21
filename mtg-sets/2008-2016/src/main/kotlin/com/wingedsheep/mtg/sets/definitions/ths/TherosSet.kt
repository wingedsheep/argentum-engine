package com.wingedsheep.mtg.sets.definitions.ths

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Theros (2013)
 *
 * Set Code: THS
 * Release Date: 2013-09-27
 */
object TherosSet : MtgSet {

    override val code = "THS"
    override val displayName = "Theros"
    override val releaseDate = "2013-09-27"
    override val block = "Theros"
    override val sealedSupported = false

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.ths.cards"
}

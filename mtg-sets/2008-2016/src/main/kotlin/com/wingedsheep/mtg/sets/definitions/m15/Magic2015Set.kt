package com.wingedsheep.mtg.sets.definitions.m15

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Magic 2015 (2014)
 *
 * Every card in the set is implemented; reprints are `Printing` rows pointing at their
 * canonical earliest printing.
 *
 * Set Code: M15
 * Release Date: 2014-07-18
 */
object Magic2015Set : MtgSet {

    override val code = "M15"
    override val displayName = "Magic 2015"
    override val releaseDate = "2014-07-18"
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

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.m15.cards"
}

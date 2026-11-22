package com.wingedsheep.mtg.sets.definitions.m14

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Magic 2014 Core Set (2013)
 *
 * Set Code: M14
 * Release Date: July 19, 2013
 */
object Magic2014Set : MtgSet {

    override val code = "M14"
    override val displayName = "Magic 2014"
    override val releaseDate = "2013-07-19"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.m14.cards"
}

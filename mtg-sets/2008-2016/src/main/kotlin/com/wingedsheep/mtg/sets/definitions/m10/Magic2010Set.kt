package com.wingedsheep.mtg.sets.definitions.m10

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Magic 2010 (2009)
 *
 * Set Code: M10
 * Release Date: July 17, 2009
 */
object Magic2010Set : MtgSet {

    override val code = "M10"
    override val displayName = "Magic 2010"
    override val releaseDate = "2009-07-17"
    override val basicLandsFallbackCode = "POR"
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.m10.cards"
}

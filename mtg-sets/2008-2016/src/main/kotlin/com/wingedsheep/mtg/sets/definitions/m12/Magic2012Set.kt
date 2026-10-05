package com.wingedsheep.mtg.sets.definitions.m12

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Magic 2012 (2011)
 *
 * Set Code: M12
 * Release Date: July 15, 2011
 */
object Magic2012Set : MtgSet {

    override val code = "M12"
    override val displayName = "Magic 2012"
    override val releaseDate = "2011-07-15"
    override val basicLandsFallbackCode = "POR"
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.m12.cards"
}

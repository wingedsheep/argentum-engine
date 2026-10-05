package com.wingedsheep.mtg.sets.definitions.conflux

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Conflux (2009)
 *
 * Set Code: CON
 * Release Date: February 6, 2009
 */
object ConfluxSet : MtgSet {

    override val code = "CON"
    override val displayName = "Conflux"
    override val releaseDate = "2009-02-06"
    override val block = "Shards of Alara"
    override val basicLandsFallbackCode = "POR"
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.conflux.cards"
}

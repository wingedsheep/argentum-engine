package com.wingedsheep.mtg.sets.definitions.s99

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Starter 1999 (1999)
 *
 * Set Code: S99
 * Release Date: 1999-07-01
 */
object Starter1999Set : MtgSet {

    override val code = "S99"
    override val displayName = "Starter 1999"
    override val releaseDate = "1999-07-01"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.s99.cards"
}

package com.wingedsheep.mtg.sets.definitions.wth

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Weatherlight Set (1997)
 *
 * Weatherlight was the third and final set in the Mirage block. It is the
 * earliest printing of several cards later reprinted in Invasion (e.g. Disrupt).
 *
 * Set Code: WTH
 * Release Date: June 9, 1997
 * Card Count: 167
 */
object WeatherlightSet : MtgSet {

    override val code = "WTH"
    override val displayName = "Weatherlight"
    override val releaseDate = "1997-06-09"
    override val block = "Mirage"
    override val basicLandsFallbackCode = "POR"
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.wth.cards"
}

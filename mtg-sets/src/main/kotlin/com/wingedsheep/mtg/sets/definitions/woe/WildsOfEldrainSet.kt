package com.wingedsheep.mtg.sets.definitions.woe

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Wilds of Eldraine (2023)
 *
 * Set Code: WOE
 * Release Date: September 8, 2023
 */
object WildsOfEldrainSet : MtgSet {

    override val code = "WOE"
    override val displayName = "Wilds of Eldraine"
    override val releaseDate = "2023-09-08"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.woe.cards"
}

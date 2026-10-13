package com.wingedsheep.mtg.sets.definitions.bro

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * The Brothers' War (2022)
 *
 * Set Code: BRO
 * Release Date: November 18, 2022
 */
object BrothersWarSet : MtgSet {

    override val code = "BRO"
    override val displayName = "The Brothers' War"
    override val releaseDate = "2022-11-18"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.bro.cards"
}

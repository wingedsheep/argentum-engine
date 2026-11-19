package com.wingedsheep.mtg.sets.definitions.jou

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Journey into Nyx (2014)
 *
 * Set Code: JOU
 * Release Date: 2014-05-02
 */
object JourneyIntoNyxSet : MtgSet {

    override val code = "JOU"
    override val displayName = "Journey into Nyx"
    override val releaseDate = "2014-05-02"
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

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.jou.cards"
}

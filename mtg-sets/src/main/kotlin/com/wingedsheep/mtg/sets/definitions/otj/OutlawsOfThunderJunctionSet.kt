package com.wingedsheep.mtg.sets.definitions.otj

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Outlaws of Thunder Junction (2024)
 *
 * Set Code: OTJ
 * Release Date: April 19, 2024
 */
object OutlawsOfThunderJunctionSet : MtgSet {

    override val code = "OTJ"
    override val displayName = "Outlaws of Thunder Junction"
    override val releaseDate = "2024-04-19"
    override val sealedSupported = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.otj.cards"
}

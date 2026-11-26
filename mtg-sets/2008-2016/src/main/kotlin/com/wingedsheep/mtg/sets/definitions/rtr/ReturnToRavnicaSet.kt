package com.wingedsheep.mtg.sets.definitions.rtr

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Return to Ravnica (2012)
 *
 * Every card of the set is implemented; cards first printed elsewhere carry a `Printing` row.
 *
 * Set Code: RTR
 * Release Date: 2012-10-05
 */
object ReturnToRavnicaSet : MtgSet {

    override val code = "RTR"
    override val displayName = "Return to Ravnica"
    override val releaseDate = "2012-10-05"
    override val block = "Return to Ravnica"
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

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.rtr.cards"
}

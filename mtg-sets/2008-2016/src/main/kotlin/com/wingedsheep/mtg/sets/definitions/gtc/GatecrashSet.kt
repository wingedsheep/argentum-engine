package com.wingedsheep.mtg.sets.definitions.gtc

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Gatecrash (2013)
 *
 * Every card of the set is implemented; reprints carry `Printing` rows.
 *
 * Set Code: GTC
 * Release Date: 2013-02-01
 */
object GatecrashSet : MtgSet {

    override val code = "GTC"
    override val displayName = "Gatecrash"
    override val releaseDate = "2013-02-01"
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

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.gtc.cards"
}

package com.wingedsheep.mtg.sets.definitions.mkm

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Murders at Karlov Manor (2024)
 *
 * Set Code: MKM
 * Release Date: February 9, 2024
 */
object MurdersAtKarlovManorSet : MtgSet {

    override val code = "MKM"
    override val displayName = "Murders at Karlov Manor"
    override val releaseDate = "2024-02-09"
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

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.mkm.cards"
}

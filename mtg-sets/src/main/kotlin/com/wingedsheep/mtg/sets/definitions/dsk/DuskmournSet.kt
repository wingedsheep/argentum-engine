package com.wingedsheep.mtg.sets.definitions.dsk

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Duskmourn: House of Horror (2024)
 *
 * Set Code: DSK
 * Release Date: September 27, 2024
 */
object DuskmournSet : MtgSet {

    override val code = "DSK"
    override val displayName = "Duskmourn: House of Horror"
    override val releaseDate = "2024-09-27"
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

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.dsk.cards"
}

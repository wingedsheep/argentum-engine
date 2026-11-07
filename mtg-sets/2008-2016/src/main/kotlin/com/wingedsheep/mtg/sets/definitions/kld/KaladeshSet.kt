package com.wingedsheep.mtg.sets.definitions.kld

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Kaladesh (2016)
 *
 * Every card is implemented: canonicals for cards first printed here, `Printing` rows for
 * the reprints.
 *
 * Set Code: KLD
 * Release Date: 2016-09-30
 */
object KaladeshSet : MtgSet {

    override val code = "KLD"
    override val displayName = "Kaladesh"
    override val releaseDate = "2016-09-30"
    override val block = "Kaladesh"
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

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.kld.cards"
}

package com.wingedsheep.mtg.sets.definitions.m19

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Core Set 2019 (2018)
 *
 * Every booster card is implemented; Nexus of Fate (Buy-a-Box promo) is an extra.
 *
 * Set Code: M19
 * Release Date: 2018-07-13
 */
object CoreSet2019Set : MtgSet {

    override val code = "M19"
    override val displayName = "Core Set 2019"
    override val releaseDate = "2018-07-13"
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

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.m19.cards"
}

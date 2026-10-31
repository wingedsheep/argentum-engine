package com.wingedsheep.mtg.sets.definitions.m20

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Core Set 2020 (2019)
 *
 * Every card is implemented: canonicals first printed here, plus `Printing` rows for reprints.
 *
 * Set Code: M20
 * Release Date: 2019-07-12
 */
object CoreSet2020Set : MtgSet {

    override val code = "M20"
    override val displayName = "Core Set 2020"
    override val releaseDate = "2019-07-12"
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

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.m20.cards"
}

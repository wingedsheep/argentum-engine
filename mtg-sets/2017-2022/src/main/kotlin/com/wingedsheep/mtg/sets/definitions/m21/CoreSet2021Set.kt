package com.wingedsheep.mtg.sets.definitions.m21

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Core Set 2021 (2020)
 *
 * Set Code: M21
 * Release Date: July 3, 2020
 */
object CoreSet2021Set : MtgSet {

    override val code = "M21"
    override val displayName = "Core Set 2021"
    override val releaseDate = "2020-07-03"
    override val basicLandsFallbackCode = "POR"
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.m21.cards"
}

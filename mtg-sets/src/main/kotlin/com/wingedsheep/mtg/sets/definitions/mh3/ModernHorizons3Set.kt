package com.wingedsheep.mtg.sets.definitions.mh3

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Modern Horizons 3
 *
 * Set Code: MH3
 */
object ModernHorizons3Set : MtgSet {

    override val code = "MH3"
    override val displayName = "Modern Horizons 3"
    override val releaseDate = "2024-06-14"
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.mh3.cards"
}

package com.wingedsheep.mtg.sets.definitions.m3c

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Modern Horizons 3 Commander (2024)
 *
 * Commander preconstructed decks released alongside Modern Horizons 3.
 *
 * Set Code: M3C
 * Release Date: June 14, 2024
 */
object ModernHorizons3CommanderSet : MtgSet {

    override val code = "M3C"
    override val displayName = "Modern Horizons 3 Commander"
    override val releaseDate = "2024-06-14"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.m3c.cards"
}

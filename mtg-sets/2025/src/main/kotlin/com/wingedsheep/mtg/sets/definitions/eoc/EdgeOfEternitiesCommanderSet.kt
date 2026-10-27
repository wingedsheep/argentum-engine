package com.wingedsheep.mtg.sets.definitions.eoc

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Edge of Eternities Commander (2025)
 *
 * Commander preconstructed decks released alongside Edge of Eternities.
 *
 * Set Code: EOC
 * Release Date: August 1, 2025
 */
object EdgeOfEternitiesCommanderSet : MtgSet {

    override val code = "EOC"
    override val displayName = "Edge of Eternities Commander"
    override val releaseDate = "2025-08-01"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.eoc.cards"
}

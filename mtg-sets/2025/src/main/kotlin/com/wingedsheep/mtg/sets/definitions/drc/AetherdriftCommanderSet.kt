package com.wingedsheep.mtg.sets.definitions.drc

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Aetherdrift Commander (2025)
 *
 * Commander preconstructed decks released alongside Aetherdrift.
 *
 * Set Code: DRC
 * Release Date: February 14, 2025
 */
object AetherdriftCommanderSet : MtgSet {

    override val code = "DRC"
    override val displayName = "Aetherdrift Commander"
    override val releaseDate = "2025-02-14"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.drc.cards"
}

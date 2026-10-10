package com.wingedsheep.mtg.sets.definitions.tdc

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Tarkir: Dragonstorm Commander (2025)
 *
 * Commander preconstructed decks released alongside Tarkir: Dragonstorm.
 *
 * Set Code: TDC
 * Release Date: April 11, 2025
 */
object TarkirDragonstormCommanderSet : MtgSet {

    override val code = "TDC"
    override val displayName = "Tarkir: Dragonstorm Commander"
    override val releaseDate = "2025-04-11"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.tdc.cards"
}

package com.wingedsheep.mtg.sets.definitions.lcc

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * The Lost Caverns of Ixalan Commander (2023)
 *
 * Commander preconstructed decks released alongside The Lost Caverns of Ixalan.
 *
 * Set Code: LCC
 * Release Date: November 17, 2023
 */
object LostCavernsOfIxalanCommanderSet : MtgSet {

    override val code = "LCC"
    override val displayName = "The Lost Caverns of Ixalan Commander"
    override val releaseDate = "2023-11-17"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.lcc.cards"
}

package com.wingedsheep.mtg.sets.definitions.woc

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Wilds of Eldraine Commander (2023)
 *
 * Commander preconstructed decks released alongside Wilds of Eldraine.
 *
 * Set Code: WOC
 * Release Date: September 8, 2023
 */
object WildsOfEldraineCommanderSet : MtgSet {

    override val code = "WOC"
    override val displayName = "Wilds of Eldraine Commander"
    override val releaseDate = "2023-09-08"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.woc.cards"
}

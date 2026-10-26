package com.wingedsheep.mtg.sets.definitions.onc

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Phyrexia: All Will Be One Commander (2023)
 *
 * Commander preconstructed decks released alongside Phyrexia: All Will Be One.
 *
 * Set Code: ONC
 * Release Date: February 10, 2023
 */
object PhyrexiaAllWillBeOneCommanderSet : MtgSet {

    override val code = "ONC"
    override val displayName = "Phyrexia: All Will Be One Commander"
    override val releaseDate = "2023-02-10"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.onc.cards"
}

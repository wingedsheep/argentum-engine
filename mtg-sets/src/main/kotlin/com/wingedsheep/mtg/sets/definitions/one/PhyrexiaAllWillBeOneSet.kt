package com.wingedsheep.mtg.sets.definitions.one

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Phyrexia: All Will Be One (2023)
 *
 * Set Code: ONE
 * Release Date: February 10, 2023
 */
object PhyrexiaAllWillBeOneSet : MtgSet {

    override val code = "ONE"
    override val displayName = "Phyrexia: All Will Be One"
    override val releaseDate = "2023-02-10"
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.one.cards"
}

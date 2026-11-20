package com.wingedsheep.mtg.sets.definitions.bng

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Born of the Gods
 *
 * Set Code: BNG
 */
object BornOfTheGodsSet : MtgSet {

    override val code = "BNG"
    override val displayName = "Born of the Gods"
    override val releaseDate = "2014-02-07"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.bng.cards"
}

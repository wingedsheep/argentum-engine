package com.wingedsheep.mtg.sets.definitions.fut

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Future Sight (2007)
 *
 * Scaffolded to hold the canonical [CardDefinition]s of cards whose earliest real-expansion
 * printing is Future Sight (e.g. Mass of Ghouls), with later sets contributing reprint
 * [Printing] rows.
 *
 * Set Code: FUT
 * Release Date: May 4, 2007
 */
object FutureSightSet : MtgSet {

    override val code = "FUT"
    override val displayName = "Future Sight"
    override val releaseDate = "2007-05-04"
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.fut.cards"
}

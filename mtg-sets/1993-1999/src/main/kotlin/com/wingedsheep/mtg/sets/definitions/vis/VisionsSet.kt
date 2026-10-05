package com.wingedsheep.mtg.sets.definitions.vis

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Visions Set (1997)
 *
 * Visions was the second set in the Mirage block, a stand-alone expansion set
 * on the plane of Jamuraa. It continued the Mirage block's themes and is the
 * earliest printing of several cards later reprinted in Invasion.
 *
 * Set Code: VIS
 * Release Date: February 3, 1997
 * Card Count: 167
 */
object VisionsSet : MtgSet {

    override val code = "VIS"
    override val displayName = "Visions"
    override val releaseDate = "1997-02-03"
    override val block = "Mirage"
    override val basicLandsFallbackCode = "POR"
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.vis.cards"
}

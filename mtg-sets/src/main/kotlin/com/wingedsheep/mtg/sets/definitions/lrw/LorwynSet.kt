package com.wingedsheep.mtg.sets.definitions.lrw

import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Lorwyn Set (2007)
 *
 * Lorwyn is the first set in the Lorwyn block, set on a sun-soaked,
 * tribal-heavy plane of eternal day. Introduced Planeswalker cards and
 * the Changeling, Champion, and Evoke mechanics.
 *
 * Set Code: LRW
 * Release Date: October 12, 2007
 * Card Count: 301
 */
object LorwynSet : MtgSet {

    override val code = "LRW"
    override val displayName = "Lorwyn"
    override val block = "Lorwyn"
    override val basicLandsFallback = PortalSet
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.lrw.cards"
}

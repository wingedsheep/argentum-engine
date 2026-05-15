package com.wingedsheep.mtg.sets.definitions.tmp

import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet

/**
 * Tempest Set (1997)
 *
 * Tempest was the first set in the Tempest block, set on the plane of Rath.
 * It introduced Shadow, Buyback, and Slivers, and established the modern
 * keyword templating that's still in use today.
 *
 * Set Code: TMP
 * Release Date: October 14, 1997
 * Card Count: 350
 */
object TempestSet : MtgSet {

    override val code = "TMP"
    override val displayName = "Tempest"
    override val block = "Tempest"
    override val basicLandsFallback = PortalSet
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.tmp.cards"
}

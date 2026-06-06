package com.wingedsheep.mtg.sets.definitions.pls

import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Planeshift (2001)
 *
 * Set Code: PLS
 * Release Date: 2001-02-05
 *
 * Scaffolded as the canonical home for cards reprinted in later sets (e.g. Eighth
 * Edition). Only the cards relocated here so far are implemented; the set is
 * otherwise incomplete.
 */
object PlaneshiftSet : MtgSet {

    override val code = "PLS"
    override val displayName = "Planeshift"
    override val releaseDate = "2001-02-05"
    override val block = "Invasion"
    override val basicLandsFallback = PortalSet
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.pls.cards"
}

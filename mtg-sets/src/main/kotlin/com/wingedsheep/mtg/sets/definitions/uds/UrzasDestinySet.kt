package com.wingedsheep.mtg.sets.definitions.uds

import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Urza's Destiny (1999)
 *
 * Set Code: UDS
 * Release Date: 1999-06-07
 *
 * Scaffolded as the canonical home for cards reprinted in later sets (e.g. Eighth
 * Edition). Only the cards relocated here so far are implemented; the set is
 * otherwise incomplete.
 */
object UrzasDestinySet : MtgSet {

    override val code = "UDS"
    override val displayName = "Urza's Destiny"
    override val releaseDate = "1999-06-07"
    override val block = "Urza's Saga"
    override val basicLandsFallback = PortalSet
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.uds.cards"
}

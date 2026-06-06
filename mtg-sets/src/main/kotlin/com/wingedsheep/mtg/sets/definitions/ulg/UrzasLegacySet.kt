package com.wingedsheep.mtg.sets.definitions.ulg

import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Urza's Legacy (1999)
 *
 * Set Code: ULG
 * Release Date: 1999-02-15
 *
 * Scaffolded as the canonical home for cards reprinted in later sets (e.g. Eighth
 * Edition). Only the cards relocated here so far are implemented; the set is
 * otherwise incomplete.
 */
object UrzasLegacySet : MtgSet {

    override val code = "ULG"
    override val displayName = "Urza's Legacy"
    override val releaseDate = "1999-02-15"
    override val block = "Urza's Saga"
    override val basicLandsFallback = PortalSet
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.ulg.cards"
}

package com.wingedsheep.mtg.sets.definitions.pcy

import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Prophecy (2000)
 *
 * Set Code: PCY
 * Release Date: 2000-06-05
 *
 * Scaffolded as the canonical home for cards reprinted in later sets (e.g. Eighth
 * Edition). Only the cards relocated here so far are implemented; the set is
 * otherwise incomplete.
 */
object ProphecySet : MtgSet {

    override val code = "PCY"
    override val displayName = "Prophecy"
    override val releaseDate = "2000-06-05"
    override val block = "Masques"
    override val basicLandsFallback = PortalSet
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.pcy.cards"
}

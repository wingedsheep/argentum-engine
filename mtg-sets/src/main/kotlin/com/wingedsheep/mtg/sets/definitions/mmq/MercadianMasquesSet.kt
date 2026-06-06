package com.wingedsheep.mtg.sets.definitions.mmq

import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Mercadian Masques (1999)
 *
 * Set Code: MMQ
 * Release Date: 1999-10-04
 *
 * Scaffolded as the canonical home for cards reprinted in later sets (e.g. Eighth
 * Edition). Only the cards relocated here so far are implemented; the set is
 * otherwise incomplete.
 */
object MercadianMasquesSet : MtgSet {

    override val code = "MMQ"
    override val displayName = "Mercadian Masques"
    override val releaseDate = "1999-10-04"
    override val block = "Masques"
    override val basicLandsFallback = PortalSet
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.mmq.cards"
}

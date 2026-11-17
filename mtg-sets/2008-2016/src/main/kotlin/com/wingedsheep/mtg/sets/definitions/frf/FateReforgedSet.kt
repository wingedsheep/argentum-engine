package com.wingedsheep.mtg.sets.definitions.frf

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Fate Reforged Set (2015)
 *
 * The second set in the Khans of Tarkir block, featuring the Manifest mechanic,
 * the Dragons cycle, and the five Sieges (Outpost Siege, etc.) with a Khans /
 * Dragons mode choice at entry.
 *
 * Set Code: FRF
 * Release Date: January 23, 2015
 */
object FateReforgedSet : MtgSet {

    override val code = "FRF"
    override val displayName = "Fate Reforged"
    override val releaseDate = "2015-01-23"
    override val sealedSupported = false

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.frf.cards"
}

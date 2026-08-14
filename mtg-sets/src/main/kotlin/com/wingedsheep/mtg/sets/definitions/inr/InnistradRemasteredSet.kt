package com.wingedsheep.mtg.sets.definitions.inr

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Innistrad Remastered (2025)
 *
 * Set Code: INR
 * Release Date: January 24, 2025
 */
object InnistradRemasteredSet : MtgSet {

    override val code = "INR"
    override val displayName = "Innistrad Remastered"
    override val releaseDate = "2025-01-24"
    // Complete as of Invasion of Innistrad (290/290) — the flag is what gates `fullyImplemented`,
    // so leaving it set would keep a finished set out of the lobby's set picker.

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.inr.cards"
}

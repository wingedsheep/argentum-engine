package com.wingedsheep.mtg.sets.definitions.trk

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Star Trek (2026)
 *
 * Scaffolded to hold the canonical [CardDefinition]s of cards whose earliest real printing is
 * Star Trek, with later sets contributing reprint [Printing] rows.
 *
 * Set Code: TRK
 * Release Date: 2026-11-13
 */
object StarTrekSet : MtgSet {

    override val code = "TRK"
    override val displayName = "Star Trek"
    override val releaseDate = "2026-11-13"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.trk.cards"
}

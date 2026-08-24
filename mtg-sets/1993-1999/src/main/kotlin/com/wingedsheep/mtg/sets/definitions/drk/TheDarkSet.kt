package com.wingedsheep.mtg.sets.definitions.drk

import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * The Dark (1994)
 *
 * Set Code: DRK
 * Release Date: August 1, 1994
 *
 * Scaffolded to hold the canonical [CardDefinition]s of cards whose earliest real-expansion
 * printing is The Dark (e.g. Bog Imp), with later sets contributing reprint [Printing] rows.
 * Every card in the set is one of those canonicals — The Dark introduced no reprints — so
 * [printings] is empty, and the set has no basic lands of its own (hence [basicLandsFallback]).
 *
 * Since filled in: all 119 cards are implemented and field-verified against Scryfall, so the set
 * is draftable (not `incomplete`).
 */
object TheDarkSet : MtgSet {

    override val code = "DRK"
    override val displayName = "The Dark"
    override val releaseDate = "1994-08-01"
    override val basicLandsFallback = PortalSet

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.drk.cards"
}

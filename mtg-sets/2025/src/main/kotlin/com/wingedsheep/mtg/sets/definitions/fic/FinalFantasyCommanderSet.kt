package com.wingedsheep.mtg.sets.definitions.fic

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Final Fantasy Commander (2025)
 *
 * Commander preconstructed decks released alongside Final Fantasy. Scaffolded so that Tataru Taru
 * — first printed here — can hold its canonical [CardDefinition] in its earliest real printing.
 * The cards this product reprints that are already implemented carry [Printing] rows here; the
 * rest is still unauthored.
 *
 * [sealedSupported] stays false: Commander precons are fixed 100-card decks, not a draftable product.
 *
 * Set Code: FIC
 * Release Date: 2025-06-13
 */
object FinalFantasyCommanderSet : MtgSet {

    override val code = "FIC"
    override val displayName = "Final Fantasy Commander"
    override val releaseDate = "2025-06-13"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.fic.cards"
}

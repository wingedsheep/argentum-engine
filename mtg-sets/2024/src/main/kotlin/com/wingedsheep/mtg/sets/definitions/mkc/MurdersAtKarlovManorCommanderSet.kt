package com.wingedsheep.mtg.sets.definitions.mkc

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Murders at Karlov Manor Commander (2024)
 *
 * Commander preconstructed decks. Scaffolded to hold the canonical [CardDefinition] for cards
 * first printed here (e.g. Trouble in Pairs); reprint rows for the already-implemented cards it
 * reprints are added as cards need them.
 *
 * [sealedSupported] stays false: Commander precons are fixed 100-card decks, not a draftable product.
 *
 * Set Code: MKC
 * Release Date: 2024-02-09
 */
object MurdersAtKarlovManorCommanderSet : MtgSet {

    override val code = "MKC"
    override val displayName = "Murders at Karlov Manor Commander"
    override val releaseDate = "2024-02-09"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.mkc.cards"
}

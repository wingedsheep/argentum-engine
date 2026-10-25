package com.wingedsheep.mtg.sets.definitions.`40k`

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Warhammer 40,000 Commander (2022)
 *
 * Universes Beyond Commander preconstructed decks. Scaffolded to hold the canonical
 * [CardDefinition] for cards first printed here (e.g. Commissar Severina Raine) and the
 * [Printing] rows of the already-implemented cards it reprints.
 *
 * [sealedSupported] stays false: Commander precons are fixed 100-card decks, not a draftable product.
 *
 * Set Code: 40K
 * Release Date: 2022-10-07
 */
object Warhammer40000CommanderSet : MtgSet {

    override val code = "40K"
    override val displayName = "Warhammer 40,000 Commander"
    override val releaseDate = "2022-10-07"
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

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.40k.cards"
}

package com.wingedsheep.mtg.sets.definitions.dmc

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Dominaria United Commander (2022)
 *
 * Commander preconstructed decks. Scaffolded to hold the canonical [CardDefinition] for cards
 * first printed here (e.g. Dihada, Binder of Wills; Shanid, Sleepers' Scourge) and the
 * [Printing] rows of the already-implemented cards it reprints.
 *
 * [sealedSupported] stays false: Commander precons are fixed 100-card decks, not a draftable product.
 *
 * Set Code: DMC
 * Release Date: 2022-09-09
 */
object DominariaUnitedCommanderSet : MtgSet {

    override val code = "DMC"
    override val displayName = "Dominaria United Commander"
    override val releaseDate = "2022-09-09"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.dmc.cards"
}

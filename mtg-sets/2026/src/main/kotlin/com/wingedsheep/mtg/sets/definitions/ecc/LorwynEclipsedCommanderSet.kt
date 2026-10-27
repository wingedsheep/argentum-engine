package com.wingedsheep.mtg.sets.definitions.ecc

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Lorwyn Eclipsed Commander (2026)
 *
 * Commander preconstructed decks released alongside Lorwyn Eclipsed.
 *
 * Set Code: ECC
 * Release Date: January 23, 2026
 */
object LorwynEclipsedCommanderSet : MtgSet {

    override val code = "ECC"
    override val displayName = "Lorwyn Eclipsed Commander"
    override val releaseDate = "2026-01-23"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.ecc.cards"
}

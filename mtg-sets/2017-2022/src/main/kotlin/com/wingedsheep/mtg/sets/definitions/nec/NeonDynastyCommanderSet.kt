package com.wingedsheep.mtg.sets.definitions.nec

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Neon Dynasty Commander (2022)
 *
 * Commander preconstructed decks released alongside Kamigawa: Neon Dynasty.
 *
 * Set Code: NEC
 * Release Date: February 18, 2022
 */
object NeonDynastyCommanderSet : MtgSet {

    override val code = "NEC"
    override val displayName = "Neon Dynasty Commander"
    override val releaseDate = "2022-02-18"
    override val sealedSupported = false

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.nec.cards"
}

package com.wingedsheep.mtg.sets.definitions.fem

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Fallen Empires (1994)
 *
 * Scaffolded to hold the canonical [CardDefinition]s of cards whose earliest real-expansion
 * printing is Fallen Empires (e.g. Icatian Priest), with later sets contributing reprint
 * [Printing] rows.
 *
 * Set Code: FEM
 * Release Date: November 1, 1994
 */
object FallenEmpiresSet : MtgSet {

    override val code = "FEM"
    override val displayName = "Fallen Empires"
    override val releaseDate = "1994-11-01"
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

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.fem.cards"
}

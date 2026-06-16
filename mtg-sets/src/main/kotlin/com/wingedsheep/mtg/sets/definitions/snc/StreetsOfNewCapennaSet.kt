package com.wingedsheep.mtg.sets.definitions.snc

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Streets of New Capenna (2022)
 *
 * Set Code: SNC
 * Release Date: April 29, 2022
 *
 * Scaffolded to hold the canonical [CardDefinition] for cards whose earliest real
 * printing is SNC (e.g. Fake Your Own Death, reprinted in Outlaws of Thunder Junction).
 */
object StreetsOfNewCapennaSet : MtgSet {

    override val code = "SNC"
    override val displayName = "Streets of New Capenna"
    override val releaseDate = "2022-04-29"
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

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.snc.cards"
}

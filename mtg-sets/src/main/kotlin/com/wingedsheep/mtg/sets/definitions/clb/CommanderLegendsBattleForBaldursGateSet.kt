package com.wingedsheep.mtg.sets.definitions.clb

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Commander Legends: Battle for Baldur's Gate (2022)
 *
 * A Commander-draft set. Scaffolded to hold the canonical [CardDefinition] for cards that first
 * appeared here and are reprinted in later sets (e.g. Carnelian Orb of Dragonkind, reprinted in
 * Foundations).
 *
 * Set Code: CLB
 * Release Date: June 10, 2022
 */
object CommanderLegendsBattleForBaldursGateSet : MtgSet {

    override val code = "CLB"
    override val displayName = "Commander Legends: Battle for Baldur's Gate"
    override val releaseDate = "2022-06-10"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.clb.cards"
}

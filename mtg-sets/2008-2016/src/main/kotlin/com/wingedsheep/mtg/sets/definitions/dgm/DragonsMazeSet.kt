package com.wingedsheep.mtg.sets.definitions.dgm

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Dragon's Maze
 *
 * Set Code: DGM
 */
object DragonsMazeSet : MtgSet {

    override val code = "DGM"
    override val displayName = "Dragon's Maze"
    override val releaseDate = "2013-05-03"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.dgm.cards"
}

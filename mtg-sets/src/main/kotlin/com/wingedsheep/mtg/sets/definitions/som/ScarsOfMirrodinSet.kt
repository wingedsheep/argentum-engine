package com.wingedsheep.mtg.sets.definitions.som

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Scars of Mirrodin (2010)
 *
 * Set Code: SOM
 * Release Date: October 1, 2010
 */
object ScarsOfMirrodinSet : MtgSet {

    override val code = "SOM"
    override val displayName = "Scars of Mirrodin"
    override val releaseDate = "2010-10-01"
    override val block = "Scars of Mirrodin"
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.som.cards"
}

package com.wingedsheep.mtg.sets.definitions.moc

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * March of the Machine Commander (2023)
 *
 * Commander preconstructed decks released alongside March of the Machine.
 *
 * Set Code: MOC
 * Release Date: April 21, 2023
 */
object MarchOfTheMachineCommanderSet : MtgSet {

    override val code = "MOC"
    override val displayName = "March of the Machine Commander"
    override val releaseDate = "2023-04-21"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.moc.cards"
}

package com.wingedsheep.mtg.sets.definitions.usg

import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Urza's Saga Set (1998)
 *
 * Urza's Saga is the first set in the Urza block, set on Dominaria and
 * focused on Urza Planeswalker's preparations for the Phyrexian Invasion.
 * Infamous for being one of the most powerful sets ever printed, kicking
 * off the "Combo Winter" Standard era.
 *
 * Set Code: USG
 * Release Date: October 12, 1998
 * Card Count: 350
 */
object UrzasSagaSet : MtgSet {

    override val code = "USG"
    override val displayName = "Urza's Saga"
    override val block = "Urza"
    override val basicLandsFallback = PortalSet
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.usg.cards"
}

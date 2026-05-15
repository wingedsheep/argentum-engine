package com.wingedsheep.mtg.sets.definitions.roe

import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Rise of the Eldrazi Set (2010)
 *
 * Rise of the Eldrazi is the third set in the Zendikar block, introducing
 * the Eldrazi as colorless titans and the Level Up, Rebound, and Totem
 * Armor mechanics. The Eldrazi titans Emrakul, Kozilek, and Ulamog
 * debuted here.
 *
 * Set Code: ROE
 * Release Date: April 23, 2010
 * Card Count: 248
 */
object RiseOfTheEldraziSet : MtgSet {

    override val code = "ROE"
    override val displayName = "Rise of the Eldrazi"
    override val block = "Zendikar"
    override val basicLandsFallback = PortalSet
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.roe.cards"
}

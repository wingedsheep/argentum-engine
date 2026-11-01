package com.wingedsheep.mtg.sets.definitions.war

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * War of the Spark (2019)
 *
 * The conclusion of the Bolas arc, themed around planeswalkers, amass, and proliferate.
 *
 * Set Code: WAR
 * Release Date: May 3, 2019
 */
object WarOfTheSparkSet : MtgSet {

    override val code = "WAR"
    override val displayName = "War of the Spark"
    override val releaseDate = "2019-05-03"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.war.cards"
}

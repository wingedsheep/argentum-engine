package com.wingedsheep.mtg.sets.definitions.leg

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Legends (1994)
 *
 * The first large expansion built entirely of new cards. Scaffolded here to hold the
 * canonical [CardDefinition]s of cards whose earliest real-expansion printing is Legends
 * (e.g. Holy Day), with later sets contributing reprint [Printing] rows. Legends printed no
 * basic lands, so sealed/draft deckbuilding borrows Portal's ([basicLandsFallbackCode]).
 * Boosters are the classic 15-card pack (11 commons, 3 uncommons, 1 rare).
 *
 * Set Code: LEG
 * Release Date: June 1, 1994
 */
object LegendsSet : MtgSet {

    override val code = "LEG"
    override val displayName = "Legends"
    override val releaseDate = "1994-06-01"
    override val basicLandsFallbackCode = "POR"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.leg.cards"
}

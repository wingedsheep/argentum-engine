package com.wingedsheep.mtg.sets.definitions.avr

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Avacyn Restored Set (2012)
 *
 * Avacyn Restored is the third and final set in the original Innistrad block.
 * It introduced Miracle and Soulbond, and centred on Avacyn's return and the
 * banishment of demons from Innistrad.
 *
 * Set Code: AVR
 * Release Date: May 4, 2012
 * Card Count: 244
 */
object AvacynRestoredSet : MtgSet {

    override val code = "AVR"
    override val displayName = "Avacyn Restored"
    override val releaseDate = "2012-05-04"
    override val block = "Innistrad"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    /** Avacyn Restored's own basics: three arts each of the five types, collector numbers 230-244. */
    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.avr.cards"
}

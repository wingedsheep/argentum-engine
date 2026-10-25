package com.wingedsheep.mtg.sets.definitions.stx

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Strixhaven: School of Mages (2021)
 *
 * The set built around the five mage colleges of Strixhaven University, featuring
 * the Learn / Lesson mechanic, Magecraft, and the Mystical Archive reprints.
 *
 * Set Code: STX
 * Release Date: April 23, 2021
 */
object StrixhavenSchoolOfMagesSet : MtgSet {

    override val code = "STX"
    override val displayName = "Strixhaven: School of Mages"
    override val releaseDate = "2021-04-23"
    override val sealedSupported = false

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.stx.cards"
}

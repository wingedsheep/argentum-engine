package com.wingedsheep.mtg.sets.definitions.soi

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Shadows over Innistrad (2016)
 *
 * Set Code: SOI
 * Release Date: April 8, 2016
 */
object ShadowsOverInnistradSet : MtgSet {

    override val code = "SOI"
    override val displayName = "Shadows over Innistrad"
    override val releaseDate = "2016-04-08"
    override val block = "Shadows over Innistrad"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.soi.cards"
}

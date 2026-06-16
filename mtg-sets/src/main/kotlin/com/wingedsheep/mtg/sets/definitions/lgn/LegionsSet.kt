package com.wingedsheep.mtg.sets.definitions.lgn

import com.wingedsheep.mtg.sets.definitions.ons.OnslaughtSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Legions Set (2003)
 *
 * Legions is the second set in the Onslaught block, notable for being the only
 * set in Magic history to consist entirely of creature cards. It features tribal
 * themes and the morph mechanic.
 *
 * Set Code: LGN
 * Release Date: February 3, 2003
 * Card Count: 145
 */
object LegionsSet : MtgSet {

    override val code = "LGN"
    override val displayName = "Legions"
    override val releaseDate = "2003-02-03"
    override val block = "Onslaught"
    override val basicLandsFallback = OnslaughtSet
    override val sealedSupported = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.lgn.cards"
}

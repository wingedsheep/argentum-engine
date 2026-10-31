package com.wingedsheep.mtg.sets.definitions.iko

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Ikoria: Lair of Behemoths
 *
 * Set Code: IKO
 */
object IkoriaSet : MtgSet {

    override val code = "IKO"
    override val displayName = "Ikoria: Lair of Behemoths"
    override val releaseDate = "2020-04-24"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.iko.cards"
}

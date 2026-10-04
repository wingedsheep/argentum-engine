package com.wingedsheep.mtg.sets.definitions.cn2

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Conspiracy: Take the Crown (2016)
 *
 * Multiplayer draft set featuring the monarch, conspiracies, and draft-matters cards.
 * Scaffolded to hold the canonical definitions of cards it printed first; it is not
 * draftable here, since conspiracies and draft-matters cards have no engine support.
 *
 * Set Code: CN2
 * Release Date: August 26, 2016
 */
object ConspiracyTakeTheCrownSet : MtgSet {

    override val code = "CN2"
    override val displayName = "Conspiracy: Take the Crown"
    override val releaseDate = "2016-08-26"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.cn2.cards"
}

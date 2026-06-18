package com.wingedsheep.mtg.sets.definitions.eld

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Throne of Eldraine (2019)
 *
 * Set Code: ELD
 * Release Date: October 4, 2019
 *
 * Scaffolded as the canonical home for cards whose earliest real expansion printing is ELD
 * (later reprinted in newer sets). Only the cards implemented so far live here; the set is marked
 * [incomplete].
 */
object ThroneOfEldraineSet : MtgSet {

    override val code = "ELD"
    override val displayName = "Throne of Eldraine"
    override val releaseDate = "2019-10-04"
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.eld.cards"
}

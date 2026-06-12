package com.wingedsheep.mtg.sets.definitions.`5dn`

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Fifth Dawn (2004)
 *
 * mtgish-tooling auto-generated seed: the cards the emitter can render whole (complete renders),
 * plus Printing(...) rows for cards whose canonical earliest printing lives in another set.
 * Intentionally incomplete relative to the official set.
 *
 * Set Code: 5DN
 * Release Date: 2004-06-04
 */
object FifthDawnSet : MtgSet {

    override val code = "5DN"
    override val displayName = "Fifth Dawn"
    override val releaseDate = "2004-06-04"
    override val block = "Mirrodin"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.5dn.cards"
}

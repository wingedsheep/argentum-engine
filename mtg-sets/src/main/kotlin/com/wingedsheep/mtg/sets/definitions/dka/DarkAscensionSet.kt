package com.wingedsheep.mtg.sets.definitions.dka

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Dark Ascension (2012)
 *
 * mtgish-tooling auto-generated seed: only the cards relocated here as their canonical
 * earliest printing. Intentionally incomplete relative to the official set.
 *
 * Set Code: DKA
 * Release Date: 2012-02-03
 */
object DarkAscensionSet : MtgSet {

    override val code = "DKA"
    override val displayName = "Dark Ascension"
    override val releaseDate = "2012-02-03"
    override val block = "Innistrad"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.dka.cards"
}

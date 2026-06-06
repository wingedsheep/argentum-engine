package com.wingedsheep.mtg.sets.definitions.rna

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Ravnica Allegiance (2019)
 *
 * The second half of the Guilds of Ravnica block — Azorius, Gruul, Orzhov, Rakdos, and Simic.
 *
 * Set Code: RNA
 * Release Date: January 25, 2019
 */
object RavnicaAllegianceSet : MtgSet {

    override val code = "RNA"
    override val displayName = "Ravnica Allegiance"
    override val releaseDate = "2019-01-25"
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.rna.cards"
}

package com.wingedsheep.mtg.sets.definitions.dst

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Darksteel (2004)
 *
 * mtgish-tooling auto-generated seed: only the cards that currently render as complete cards.
 * Intentionally incomplete relative to the official set.
 *
 * Set Code: DST
 * Release Date: 2004-02-06
 */
object DarksteelSet : MtgSet {

    override val code = "DST"
    override val displayName = "Darksteel"
    override val releaseDate = "2004-02-06"
    override val block = "Mirrodin"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.dst.cards"
}

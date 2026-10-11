package com.wingedsheep.mtg.sets.definitions.unf

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Unfinity (2022)
 *
 * Scaffolded to host the earliest real printing of its black-bordered, eternal-legal cards that
 * were later reprinted in other sets (e.g. Saw in Half, reprinted in Bloomburrow Commander).
 * Intentionally incomplete relative to the official product — the acorn-stamped cards are not
 * implemented; only cards with a canonical [CardDefinition] or a [Printing] row here are present.
 *
 * Set Code: UNF
 * Release Date: 2022-10-07
 */
object UnfinitySet : MtgSet {

    override val code = "UNF"
    override val displayName = "Unfinity"
    override val releaseDate = "2022-10-07"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.unf.cards"
}

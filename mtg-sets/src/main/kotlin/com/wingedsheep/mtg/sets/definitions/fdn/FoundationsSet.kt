package com.wingedsheep.mtg.sets.definitions.fdn

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Foundations (2024)
 *
 * Set Code: FDN
 * Release Date: November 15, 2024
 *
 * Foundations is a reprint-focused core-style set, used here primarily as a home
 * for Modern-legal staples referenced by MageZero training decks (see
 * backlog/magezero-coverage.md).
 */
object FoundationsSet : MtgSet {

    override val code = "FDN"
    override val displayName = "Foundations"
    override val releaseDate = "2024-11-15"
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

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.fdn.cards"
}

package com.wingedsheep.mtg.sets.definitions.c21

import com.wingedsheep.mtg.sets.definitions.stx.StrixhavenSchoolOfMagesSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.mtg.sets.tokens.TokenArtData
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.TokenPrinting

object Commander2021Set : MtgSet {
    override val code = "C21"
    override val displayName = "Commander 2021"
    override val releaseDate = "2021-04-23"
    override val sealedSupported = false
    override val incomplete = true

    // The Inkling Inkshield mints is a Strixhaven token; `tc21` never printed it. Only that row is
    // borrowed: declaring any art registers C21 as an art-carrying set, so the rest of Strixhaven's
    // sheet would shadow the canonical art of every other token a C21 printing mints (Thopters,
    // Myr, Food), and C21's own `tc21` sheet isn't synced for `borrowedFrom` to filter against.
    override val tokenArt: List<TokenPrinting> by lazy {
        TokenArtData.forSet(StrixhavenSchoolOfMagesSet.code).filter { it.matchesName("Inkling") }
    }
    override val cards: List<CardDefinition> by lazy { CardDiscovery.findIn(CARDS_PACKAGE) }
    override val printings: List<Printing> by lazy { CardDiscovery.findPrintingsIn(CARDS_PACKAGE) }
    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.c21.cards"
}

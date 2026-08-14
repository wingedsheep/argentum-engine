package com.wingedsheep.mtg.sets.definitions.blc

import com.wingedsheep.mtg.sets.definitions.blb.BloomburrowSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.mtg.sets.tokens.TokenArtData
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.TokenPrinting

/**
 * Bloomburrow Commander Set (2024)
 *
 * Commander preconstructed decks released alongside the Bloomburrow main set.
 *
 * Set Code: BLC
 * Release Date: August 2, 2024
 * Card Count: 312
 */
object BloomburrowCommanderSet : MtgSet {

    override val code = "BLC"
    override val displayName = "Bloomburrow Commander"
    override val releaseDate = "2024-08-02"
    override val sealedSupported = false

    // The Commander decks shipped alongside Bloomburrow and share its token sheet, so `tblc` skips
    // tokens the main set already printed — the Treasure its cards mint is a Bloomburrow token.
    override val tokenArt: List<TokenPrinting> by lazy {
        TokenArtData.borrowedFrom(BloomburrowSet.code, code)
    }

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.blc.cards"
}

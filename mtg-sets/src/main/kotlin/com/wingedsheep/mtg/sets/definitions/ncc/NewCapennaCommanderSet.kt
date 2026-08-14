package com.wingedsheep.mtg.sets.definitions.ncc

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.TokenPrinting

/**
 * New Capenna Commander (2022)
 *
 * Commander preconstructed decks released alongside Streets of New Capenna.
 *
 * Set Code: NCC
 * Release Date: April 29, 2022
 */
object NewCapennaCommanderSet : MtgSet {

    override val code = "NCC"
    override val displayName = "New Capenna Commander"
    override val releaseDate = "2022-04-29"
    override val sealedSupported = false

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    /**
     * Scryfall's `tncc` sync covers 32 New Capenna Commander tokens but not its Treasure, so
     * that one is self-hosted under `web-client/public/images/tokens/` and declared here.
     */
    override val tokenArt: List<TokenPrinting> = listOf(
        TokenPrinting(name = "Treasure", imageUri = "/images/tokens/ncc-treasure.jpeg"),
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.ncc.cards"
}

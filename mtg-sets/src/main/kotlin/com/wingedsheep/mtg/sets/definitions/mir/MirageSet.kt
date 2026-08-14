package com.wingedsheep.mtg.sets.definitions.mir

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.TokenPrinting

/**
 * Mirage Set (1996)
 *
 * Mirage was the first set in the Mirage block, set on the African-inspired
 * plane of Jamuraa. It introduced Flanking and Phasing, and was the first
 * expansion designed as part of a planned block.
 *
 * Set Code: MIR
 * Release Date: October 8, 1996
 * Card Count: 350
 */
object MirageSet : MtgSet {

    override val code = "MIR"
    override val displayName = "Mirage"
    override val releaseDate = "1996-10-08"
    override val block = "Mirage"
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

    /**
     * Mirage predates token *cards* — Scryfall has no `tmir` set to sync from — so its
     * token art is self-hosted under `web-client/public/images/tokens/` and declared here, the
     * same route Invasion, Apocalypse and Odyssey take.
     */
    override val tokenArt: List<TokenPrinting> = listOf(
        TokenPrinting(
            name = "Pegasus",
            imageUri = "/images/tokens/mir-pegasus.jpeg",
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
        ),
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.mir.cards"
}

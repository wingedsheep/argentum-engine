package com.wingedsheep.mtg.sets.definitions.inv

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.TokenPrinting
import com.wingedsheep.sdk.core.Color

/**
 * Invasion (2000)
 *
 * Set Code: INV
 * Release Date: October 2, 2000
 *
 * First set in the Invasion block. Heavy multicolor theme; home of the
 * canonical "divvy" mechanic exemplar Fact or Fiction (CR 700.3 piles).
 */
object InvasionSet : MtgSet {

    override val code = "INV"
    override val displayName = "Invasion"
    override val releaseDate = "2000-10-02"
    override val block = "Invasion"
    override val sealedSupported = true

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
     * Invasion predates token *cards* — Scryfall has no `tinv` set to sync from — so its token art
     * is self-hosted under `web-client/public/images/tokens/` and declared here. Previously each
     * token-making card repeated the path inline; the set is the right owner.
     */
    override val tokenArt: List<TokenPrinting> = listOf(
        TokenPrinting(
            name = "Saproling",
            imageUri = "/images/tokens/inv-saproling.jpeg",
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
        ),
        // Pure Reflection's token is X/X, so power/toughness stay unpinned.
        TokenPrinting(
            name = "Reflection",
            imageUri = "/images/tokens/inv-reflection.jpeg",
            colors = setOf(Color.WHITE),
        ),
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.inv.cards"
}

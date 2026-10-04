package com.wingedsheep.mtg.sets.definitions.mh3

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.TokenPrinting

/**
 * Modern Horizons 3
 *
 * Set Code: MH3
 */
object ModernHorizons3Set : MtgSet {

    override val code = "MH3"
    override val displayName = "Modern Horizons 3"
    override val releaseDate = "2024-06-14"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    /** The Spellgorger Weird minted by Ral and the Implicit Maze, in MH3's own token printing. */
    override val tokenArt: List<TokenPrinting> = listOf(
        TokenPrinting(
            name = "Spellgorger Weird",
            imageUri = "https://cards.scryfall.io/normal/front/3/3/33b63bd0-0b61-4a87-928f-95fc6b5a3150.jpg?1783911112",
        ),
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.mh3.cards"
}

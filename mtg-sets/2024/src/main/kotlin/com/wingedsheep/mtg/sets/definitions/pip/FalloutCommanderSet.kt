package com.wingedsheep.mtg.sets.definitions.pip

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.TokenPrinting

/**
 * Fallout (2024)
 *
 * The Universes Beyond: Fallout Commander decks — every card is implemented, as a canonical
 * [CardDefinition] for its new cards or a [Printing] row for its reprints.
 *
 * Set Code: PIP
 * Release Date: 2024-03-08
 */
object FalloutCommanderSet : MtgSet {

    override val code = "PIP"
    override val displayName = "Fallout"
    override val releaseDate = "2024-03-08"
    override val sealedSupported = false

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    override val basicLands by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val tokenArt = listOf(
        TokenPrinting(name = "Junk", imageUri = "https://cards.scryfall.io/normal/front/6/2/62c4a6f6-6425-4c0c-b35a-880fcab42aad.jpg?1783912041"),
        TokenPrinting(name = "Food", imageUri = "https://cards.scryfall.io/normal/front/e/5/e5d0801a-9565-419f-bb0b-23547c9dd59d.jpg?1783912042"),
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.pip.cards"
}

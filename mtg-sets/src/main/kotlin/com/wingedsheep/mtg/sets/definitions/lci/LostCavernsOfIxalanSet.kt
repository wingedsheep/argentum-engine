package com.wingedsheep.mtg.sets.definitions.lci

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.TokenPrinting

/**
 * The Lost Caverns of Ixalan Set (2023)
 *
 * Set Code: LCI
 * Release Date: November 17, 2023
 */
object LostCavernsOfIxalanSet : MtgSet {

    override val code = "LCI"
    override val displayName = "The Lost Caverns of Ixalan"
    override val releaseDate = "2023-11-17"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    /**
     * LCI's own token printings (Scryfall set `tlci`). Previously every Treasure-making LCI card
     * passed this URI to `Effects.CreateTreasure(imageUri = …)`; declaring it once here keeps art
     * out of the card scripts and means a reprint of one of those cards mints the *reprint's*
     * Treasure rather than dragging LCI art along with it.
     *
     * (Map is omitted: the predefined Map already uses the LCI printing, so no override is needed.)
     */
    override val tokenArt: List<TokenPrinting> = listOf(
        // tlci #18 — Treasure.
        TokenPrinting(
            name = "Treasure",
            imageUri = "https://cards.scryfall.io/normal/front/3/d/3dfaedeb-f8ec-4f0e-b243-c850770a86f2.jpg?1783913602",
        ),
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.lci.cards"
}

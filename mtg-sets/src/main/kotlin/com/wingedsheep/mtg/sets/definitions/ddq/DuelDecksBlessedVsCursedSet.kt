package com.wingedsheep.mtg.sets.definitions.ddq

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.TokenPrinting

/**
 * Duel Decks: Blessed vs. Cursed (2016)
 *
 * mtgish-tooling seed: only the cards relocated here as their canonical earliest printing.
 * Intentionally incomplete relative to the official set.
 *
 * Set Code: DDQ
 * Release Date: 2016-02-26
 */
object DuelDecksBlessedVsCursedSet : MtgSet {

    override val code = "DDQ"
    override val displayName = "Duel Decks: Blessed vs. Cursed"
    override val releaseDate = "2016-02-26"
    override val sealedSupported = false
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
     * DDQ #77–80. Duel decks number their tokens inside the main set rather than a separate
     * `t<code>` set, so these have real collector numbers — but a token still has no
     * `CardDefinition` and no `Printing` row, so the art lives here. Declaring them as discovered
     * `card(...)` vals instead would file them into [cards] as four mana-costless "cards".
     */
    override val tokenArt: List<TokenPrinting> = listOf(
        TokenPrinting(
            name = "Angel",
            imageUri = "https://cards.scryfall.io/normal/front/8/e/8e3a583e-2310-4284-ac44-fd28c72ec11b.jpg?1783937832",
            power = 4,
            toughness = 4,
            colors = setOf(Color.WHITE),
        ),
        TokenPrinting(
            name = "Human",
            imageUri = "https://cards.scryfall.io/normal/front/1/5/15a620da-5056-4582-8da5-2c955c3f4c0d.jpg?1783937832",
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
        ),
        TokenPrinting(
            name = "Spirit",
            imageUri = "https://cards.scryfall.io/normal/front/b/3/b38c8153-ded1-499f-929a-b7bc8a09cd5a.jpg?1783937831",
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
        ),
        TokenPrinting(
            name = "Zombie",
            imageUri = "https://cards.scryfall.io/normal/front/7/c/7c60e495-8fb7-43bb-b11d-52882c0246bc.jpg?1783937831",
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
        ),
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.ddq.cards"
}

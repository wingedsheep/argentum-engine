package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Fact or Fiction {3}{U}
 * Instant
 *
 * Reveal the top five cards of your library. An opponent separates those cards
 * into two piles. Put one pile into your hand and the other into your graveyard.
 *
 * The canonical "divvy" mechanic exemplar (CR 700.3 piles).
 */
val FactOrFiction = card("Fact or Fiction") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Reveal the top five cards of your library. An opponent separates those cards into two piles. Put one pile into your hand and the other into your graveyard."

    spell {
        effect = LibraryPatterns.factOrFiction(count = 5)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        artist = "Terese Nielsen"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7fd4d018-dcf3-4439-8445-02d66e44f7d3.jpg?1562920780"
    }
}

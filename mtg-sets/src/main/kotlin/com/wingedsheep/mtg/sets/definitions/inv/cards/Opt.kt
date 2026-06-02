package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Opt
 * {U}
 * Instant
 * Scry 1. Draw a card.
 */
val Opt = card("Opt") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Scry 1. (Look at the top card of your library. You may put that card on the bottom of your library.)\nDraw a card."

    spell {
        effect = LibraryPatterns.scry(1).then(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "64"
        artist = "John Howe"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/958262ec-8e52-40cf-a9fd-a60e42643e15.jpg?1595075944"
    }
}

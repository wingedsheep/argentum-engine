package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Tasigur's Cruelty
 * {5}{B}
 * Sorcery
 * Delve (Each card you exile from your graveyard while casting this spell pays for {1}.)
 * Each opponent discards two cards.
 */
val TasigursCruelty = card("Tasigur's Cruelty") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Delve (Each card you exile from your graveyard while casting this spell pays for {1}.)\n" +
        "Each opponent discards two cards."

    keywords(Keyword.DELVE)

    spell {
        effect = Effects.EachOpponentDiscards(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "88"
        artist = "Chris Rahn"
        flavorText = "The cruelest tortures leave no mark upon the flesh."
        imageUri = "https://cards.scryfall.io/normal/front/1/6/169748df-10ac-4db4-ba94-192345c6fad3.jpg"
    }
}

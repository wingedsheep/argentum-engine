package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Hundroog
 * {6}{G}
 * Creature — Beast
 * 4/7
 * Cycling {3}
 */
val Hundroog = card("Hundroog") {
    manaCost = "{6}{G}"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 7
    oracleText = "Cycling {3}"

    keywordAbility(KeywordAbility.cycling("{3}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "129"
        artist = "Wayne England"
        flavorText = "\"New links are being added to the food chain on a daily basis.\" —Riptide Project researcher"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f525c356-88ca-4e2e-8f06-663be101e34f.jpg?1562944359"
    }
}

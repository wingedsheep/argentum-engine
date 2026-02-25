package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Woolly Loxodon
 * {5}{G}{G}
 * Creature — Elephant Warrior
 * 6/7
 * Morph {5}{G}
 */
val WoollyLoxodon = card("Woolly Loxodon") {
    manaCost = "{5}{G}{G}"
    typeLine = "Creature — Elephant Warrior"
    power = 6
    toughness = 7
    oracleText = "Morph {5}{G}"

    morph = "{5}{G}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "158"
        artist = "Karla Ortiz"
        flavorText = "\"Even among the hardiest warriors of the Temur, loxodons are respected for their adaptation to the mountain snows.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a890c55a-8746-4233-b75a-19cc760c1e8e.jpg?1562791638"
    }
}

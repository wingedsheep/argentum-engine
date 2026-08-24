package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Zodiac Dog
 * {2}{R}
 * Creature — Dog
 * 2/2
 * Mountainwalk (This creature can't be blocked as long as defending player controls a Mountain.)
 */
val ZodiacDog = card("Zodiac Dog") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dog"
    power = 2
    toughness = 2
    oracleText = "Mountainwalk (This creature can't be blocked as long as defending player controls a Mountain.)"

    keywords(Keyword.MOUNTAINWALK)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "130"
        artist = "Qi Baocheng"
        flavorText = "\". . . Jiang Wei alone still strove with might and main: / Nine times more he fought the north—in vain. . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc61aa62-25ee-40a4-9b6e-d6277316b464.jpg"
    }
}

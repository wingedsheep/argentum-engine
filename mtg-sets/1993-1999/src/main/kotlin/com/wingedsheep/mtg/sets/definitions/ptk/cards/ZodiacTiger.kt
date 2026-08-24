package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Zodiac Tiger
 * {2}{G}{G}
 * Creature — Cat
 * 3/4
 * Forestwalk (This creature can't be blocked as long as defending player controls a Forest.)
 */
val ZodiacTiger = card("Zodiac Tiger") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat"
    power = 3
    toughness = 4
    oracleText = "Forestwalk (This creature can't be blocked as long as defending player controls a Forest.)"

    keywords(Keyword.FORESTWALK)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "164"
        artist = "Ai Desheng"
        flavorText = "\". . . Three kings no more—Chenliu, Guiming, Anle. / The fiefs and posts must now be filled anew. . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aafd78d5-df9f-46a3-a634-ebf7634a6358.jpg"
    }
}

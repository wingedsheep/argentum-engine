package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Knight of Infamy
 * {1}{B}
 * Creature — Human Knight
 * 2/1
 * Protection from white
 * Exalted
 */
val KnightOfInfamy = card("Knight of Infamy") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Knight"
    oracleText = "Protection from white (This creature can't be blocked, targeted, dealt damage, or enchanted by anything white.)\n" +
        "Exalted (Whenever a creature you control attacks alone, that creature gets +1/+1 until end of turn.)"
    power = 2
    toughness = 1

    keywordAbility(KeywordAbility.protectionFrom(Color.WHITE))
    keywords(Keyword.EXALTED)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "96"
        artist = "Peter Mohrbacher"
        flavorText = "\"Your laws, like your bones, were made to be broken.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e339853-5b6b-47b7-8d88-e9d3befb803f.jpg"
    }
}

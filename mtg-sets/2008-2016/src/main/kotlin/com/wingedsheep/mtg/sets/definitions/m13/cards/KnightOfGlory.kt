package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Knight of Glory
 * {1}{W}
 * Creature — Human Knight
 * 2/1
 * Protection from black
 * Exalted
 */
val KnightOfGlory = card("Knight of Glory") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    oracleText = "Protection from black (This creature can't be blocked, targeted, dealt damage, or enchanted by anything black.)\n" +
        "Exalted (Whenever a creature you control attacks alone, that creature gets +1/+1 until end of turn.)"
    power = 2
    toughness = 1

    keywordAbility(KeywordAbility.protectionFrom(Color.BLACK))
    keywords(Keyword.EXALTED)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Peter Mohrbacher"
        flavorText = "\"I will uphold the law, and no manner of foe will stop me.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/6/1646cb67-e0ac-4f2d-af21-618ff3613d69.jpg"
    }
}

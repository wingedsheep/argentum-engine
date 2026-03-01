package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Macetail Hystrodon
 * {6}{R}
 * Creature — Beast
 * 4/4
 * First strike, haste
 * Cycling {3}
 */
val MacetailHystrodon = card("Macetail Hystrodon") {
    manaCost = "{6}{R}"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 4
    oracleText = "First strike, haste\nCycling {3}"

    keywords(Keyword.FIRST_STRIKE, Keyword.HASTE)
    keywordAbility(KeywordAbility.cycling("{3}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "106"
        artist = "Daren Bader"
        flavorText = "The goblins tracked the hystrodon with much stealth and cunning. Then they were eaten with much pain and yelling."
        imageUri = "https://cards.scryfall.io/normal/front/8/4/8451ab3f-5d61-4f35-ab70-5a5060caf53d.jpg?1562921768"
    }
}

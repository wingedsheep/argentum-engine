package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Zodiac Monkey
 * {1}{G}
 * Creature — Monkey
 * 2/1
 * Forestwalk
 */
val ZodiacMonkey = card("Zodiac Monkey") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Monkey"
    power = 2
    toughness = 1
    oracleText = "Forestwalk (This creature can't be blocked as long as defending player controls a Forest.)"

    keywords(Keyword.FORESTWALK)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "160"
        artist = "Ai Desheng"
        flavorText = "\". . . By six offensives from the hills of Qi Kongming sought to change Han's destiny. . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e98eb0b-c3b5-4561-b8a2-f22bd0fe1115.jpg"
    }
}

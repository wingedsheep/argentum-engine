package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Zodiac Rabbit
 * {G}
 * Creature — Rabbit
 * 1/1
 */
val ZodiacRabbit = card("Zodiac Rabbit") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Rabbit"
    power = 1
    toughness = 1
    oracleText = "Forestwalk (This creature can't be blocked as long as defending player controls a Forest.)"

    keywords(Keyword.FORESTWALK)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "162"
        artist = "Ai Desheng"
        flavorText = "\". . . The world's affairs rush on, an endless stream; / A sky-told fate, infinite in reach, dooms all. . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab4211d0-deef-4113-84e0-47ce0df7a5c6.jpg"
    }
}

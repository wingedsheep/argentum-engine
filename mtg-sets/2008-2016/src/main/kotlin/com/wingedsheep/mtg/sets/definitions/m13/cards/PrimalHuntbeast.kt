package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Primal Huntbeast
 * {3}{G}
 * Creature — Beast
 * 3/3
 * Hexproof
 */
val PrimalHuntbeast = card("Primal Huntbeast") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    oracleText = "Hexproof (This creature can't be the target of spells or abilities your opponents control.)"
    power = 3
    toughness = 3

    keywords(Keyword.HEXPROOF)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "182"
        artist = "Chris Rahn"
        flavorText = "\"To the unskilled, it appears as blurred patches of distorted light. Few have seen its true form.\"\n—Garruk Wildspeaker"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb77f6a8-a9d6-4fdd-996e-70877199ebab.jpg"
    }
}

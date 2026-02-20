package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Titanic Bulvox
 * {6}{G}{G}
 * Creature — Beast
 * 7/4
 * Trample
 * Morph {4}{G}{G}{G}
 */
val TitanicBulvox = card("Titanic Bulvox") {
    manaCost = "{6}{G}{G}"
    typeLine = "Creature — Beast"
    power = 7
    toughness = 4
    oracleText = "Trample\nMorph {4}{G}{G}{G}"

    keywords(Keyword.TRAMPLE)

    morph = "{4}{G}{G}{G}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "129"
        artist = "Mark Tedin"
        flavorText = "It puts the \"bull\" in \"bulvox.\""
        imageUri = "https://cards.scryfall.io/large/front/3/f/3f42c4d7-b555-449c-a539-119c1ae62232.jpg?1562528017"
    }
}

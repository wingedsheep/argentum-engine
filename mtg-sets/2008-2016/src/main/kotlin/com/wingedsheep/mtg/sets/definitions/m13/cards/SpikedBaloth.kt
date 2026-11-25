package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Spiked Baloth
 * {3}{G}
 * Creature — Beast
 * 4/2
 * Trample
 */
val SpikedBaloth = card("Spiked Baloth") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    oracleText = "Trample (This creature can deal excess combat damage to the player or planeswalker it's attacking.)"
    power = 4
    toughness = 2

    keywords(Keyword.TRAMPLE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "192"
        artist = "Daarken"
        flavorText = "If a baloth isn't hungry, it's dead."
        imageUri = "https://cards.scryfall.io/normal/front/5/2/522777b1-a89f-4969-a962-0137018ec86c.jpg"
    }
}

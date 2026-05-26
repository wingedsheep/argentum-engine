package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bird Maiden
 * {2}{R}
 * Creature — Human Bird
 * 1/2
 * Flying
 */
val BirdMaiden = card("Bird Maiden") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Bird"
    power = 1
    toughness = 2
    oracleText = "Flying"
    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Kaja Foglio"
        flavorText = "\"Four things that never meet do here unite\nTo shed my blood and to ravage my heart,\nA radiant brow and tresses that beguile\nAnd rosy cheeks and a glittering smile.\" —The Arabian Nights, trans. Haddawy"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c1ba0b9-db01-447f-90cc-a2fc2c24146e.jpg?1562912017"
    }
}

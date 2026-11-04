package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val CarrionScreecher = card("Carrion Screecher") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Bird"
    oracleText = "Flying"
    power = 3
    toughness = 1

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "61"
        artist = "Tomasz Jedruszek"
        flavorText = "The banished have come home to roost."
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57b9e3a3-8d01-4273-9658-9ffcbc2d2297.jpg?1783936042"
    }
}

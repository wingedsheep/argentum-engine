package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val ScroungerOfSouls = card("Scrounger of Souls") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horror"
    oracleText = "Lifelink (Damage dealt by this creature also causes you to gain that much life.)"
    power = 3
    toughness = 4

    keywords(Keyword.LIFELINK)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Zezhou Chen"
        flavorText = "Yell for help, and that may answer."
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d66f3c4-5a11-4e28-a4ae-673b3b36d3ec.jpg?1783936037"
    }
}

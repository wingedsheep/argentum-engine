package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val BitterbowSharpshooters = card("Bitterbow Sharpshooters") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Jackal Archer"
    oracleText = "Reach, vigilance"
    power = 4
    toughness = 4

    keywords(Keyword.REACH, Keyword.VIGILANCE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "112"
        artist = "Bram Sels"
        flavorText = "\"Remember, brother, the eyes are always vulnerable.\"\n\"I never forgot.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8ecfcf99-be2b-4e5f-adef-1977ee5c6a0f.jpg?1783936022"
    }
}

package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val BrambleweftBehemoth = card("Brambleweft Behemoth") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    oracleText = "Trample (This creature can deal excess combat damage to the player or planeswalker it's attacking.)"
    power = 6
    toughness = 6

    keywords(Keyword.TRAMPLE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "202"
        artist = "Florian de Gesincourt"
        flavorText = "Life will find a way, or it will make one."
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c759e94-e437-4dda-af0f-6578c0a50619.jpg?1783935986"
        inBooster = false
    }
}

package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Crystal Grotto reprint in Wilds of Eldraine. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the card's earliest printing, Dominaria
 * United (`definitions/dmu/cards/CrystalGrotto.kt`); this file contributes only WOE's presentation
 * data.
 */
val CrystalGrottoReprint = Printing(
    oracleId = "f15fb0cc-8e96-4f03-94d0-b51410415afd",
    name = "Crystal Grotto",
    setCode = "WOE",
    collectorNumber = "254",
    scryfallId = "6f6f9d3d-600d-43f3-a915-612e5d53aaa1",
    artist = "Andreas Zafiratos",
    imageUri = "https://cards.scryfall.io/normal/front/6/f/6f6f9d3d-600d-43f3-a915-612e5d53aaa1.jpg?1783915057",
    releaseDate = "2023-09-08",
    rarity = Rarity.COMMON,
)

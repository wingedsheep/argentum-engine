package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Misty Rainforest reprint in Modern Horizons 2. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Zendikar (`zen`) `cards/` package; this
 * file contributes only per-printing presentation data.
 */
val MistyRainforestReprint = Printing(
    oracleId = "09dd85aa-47bc-4713-a9b9-8b52ff2285ed",
    name = "Misty Rainforest",
    setCode = "MH2",
    collectorNumber = "250",
    scryfallId = "88231c0d-0cc8-44ec-bf95-81d1710ac141",
    artist = "Shelly Wan",
    imageUri = "https://cards.scryfall.io/normal/front/8/8/88231c0d-0cc8-44ec-bf95-81d1710ac141.jpg?1783926795",
    releaseDate = "2021-06-18",
    rarity = Rarity.RARE,
    borderColor = "black",
)

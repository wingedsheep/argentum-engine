package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Misty Rainforest extended-art reprint in Modern Horizons 2. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Zendikar (`zen`) `cards/` package; this
 * file contributes only per-printing presentation data.
 */
val MistyRainforestExtendedArtReprint = Printing(
    oracleId = "09dd85aa-47bc-4713-a9b9-8b52ff2285ed",
    name = "Misty Rainforest",
    setCode = "MH2",
    collectorNumber = "477",
    scryfallId = "23d8e67a-5150-4782-98d1-da2ca79607ad",
    artist = "Shelly Wan",
    imageUri = "https://cards.scryfall.io/normal/front/2/3/23d8e67a-5150-4782-98d1-da2ca79607ad.jpg?1783926701",
    releaseDate = "2021-06-18",
    rarity = Rarity.RARE,
    frameEffects = listOf("extendedart"),
    borderColor = "black",
)

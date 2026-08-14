package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Scalding Tarn reprint in Modern Horizons 2. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Zendikar (`zen`) `cards/` package; this
 * file contributes only per-printing presentation data.
 */
val ScaldingTarnReprint = Printing(
    oracleId = "cb027150-848c-4a66-88ad-e20222304dd8",
    name = "Scalding Tarn",
    setCode = "MH2",
    collectorNumber = "254",
    scryfallId = "71e491c5-8c07-449b-b2f1-ffa052e6d311",
    artist = "Philip Straub",
    imageUri = "https://cards.scryfall.io/normal/front/7/1/71e491c5-8c07-449b-b2f1-ffa052e6d311.jpg?1783926793",
    releaseDate = "2021-06-18",
    rarity = Rarity.RARE,
    borderColor = "black",
)

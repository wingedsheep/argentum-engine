package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Scalding Tarn extended-art reprint in Modern Horizons 2. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Zendikar (`zen`) `cards/` package; this
 * file contributes only per-printing presentation data.
 */
val ScaldingTarnExtendedArtReprint = Printing(
    oracleId = "cb027150-848c-4a66-88ad-e20222304dd8",
    name = "Scalding Tarn",
    setCode = "MH2",
    collectorNumber = "478",
    scryfallId = "229ecfc9-8d6b-4fdb-9001-64dc3e4e7a3f",
    artist = "Philip Straub",
    imageUri = "https://cards.scryfall.io/normal/front/2/2/229ecfc9-8d6b-4fdb-9001-64dc3e4e7a3f.jpg?1783926705",
    releaseDate = "2021-06-18",
    rarity = Rarity.RARE,
    frameEffects = listOf("extendedart"),
    borderColor = "black",
)

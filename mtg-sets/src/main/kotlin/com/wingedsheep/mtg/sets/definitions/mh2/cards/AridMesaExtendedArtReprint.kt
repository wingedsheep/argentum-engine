package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Arid Mesa extended-art reprint in Modern Horizons 2. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Zendikar (`zen`) `cards/` package; this
 * file contributes only per-printing presentation data.
 */
val AridMesaExtendedArtReprint = Printing(
    oracleId = "c5acf2a5-40f4-433d-a74d-1cb56c521464",
    name = "Arid Mesa",
    setCode = "MH2",
    collectorNumber = "475",
    scryfallId = "54ced5cf-b51a-4dab-97f7-50fb18e5c463",
    artist = "Raymond Swanland",
    imageUri = "https://cards.scryfall.io/normal/front/5/4/54ced5cf-b51a-4dab-97f7-50fb18e5c463.jpg?1783926701",
    releaseDate = "2021-06-18",
    rarity = Rarity.RARE,
    frameEffects = listOf("extendedart"),
    borderColor = "black",
)

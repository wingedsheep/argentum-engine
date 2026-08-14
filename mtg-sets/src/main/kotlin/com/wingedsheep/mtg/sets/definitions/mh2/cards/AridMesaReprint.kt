package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Arid Mesa reprint in Modern Horizons 2. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in the Zendikar (`zen`) `cards/` package; this file contributes only per-printing
 * presentation data.
 */
val AridMesaReprint = Printing(
    oracleId = "c5acf2a5-40f4-433d-a74d-1cb56c521464",
    name = "Arid Mesa",
    setCode = "MH2",
    collectorNumber = "244",
    scryfallId = "25ac5405-df7b-4097-914a-022cb18e20d4",
    artist = "Raymond Swanland",
    imageUri = "https://cards.scryfall.io/normal/front/2/5/25ac5405-df7b-4097-914a-022cb18e20d4.jpg?1783926797",
    releaseDate = "2021-06-18",
    rarity = Rarity.RARE,
    borderColor = "black",
)

package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Marsh Flats reprint in Modern Horizons 2. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in the Zendikar (`zen`) `cards/` package; this file contributes only per-printing
 * presentation data.
 */
val MarshFlatsReprint = Printing(
    oracleId = "dab520d0-20b4-4273-ba6b-eb07f85ea433",
    name = "Marsh Flats",
    setCode = "MH2",
    collectorNumber = "248",
    scryfallId = "9db3ba6d-eb7f-4f5b-9a3b-c6239c3baa42",
    artist = "Izzy",
    imageUri = "https://cards.scryfall.io/normal/front/9/d/9db3ba6d-eb7f-4f5b-9a3b-c6239c3baa42.jpg?1783926796",
    releaseDate = "2021-06-18",
    rarity = Rarity.RARE,
    borderColor = "black",
)

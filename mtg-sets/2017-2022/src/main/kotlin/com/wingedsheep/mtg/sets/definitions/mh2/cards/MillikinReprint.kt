package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Millikin reprint in Modern Horizons 2. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in the Odyssey (`ody`) `cards/` package; this file contributes only
 * per-printing presentation data.
 */
val MillikinReprint = Printing(
    oracleId = "fa4dffda-6f04-4d0b-829d-28a1a5794dee",
    name = "Millikin",
    setCode = "MH2",
    collectorNumber = "297",
    scryfallId = "513ba8b6-9583-405f-84a5-9d2ca42f9597",
    artist = "Joe Slucher",
    imageUri = "https://cards.scryfall.io/normal/front/5/1/513ba8b6-9583-405f-84a5-9d2ca42f9597.jpg?1783926776",
    releaseDate = "2021-06-18",
    rarity = Rarity.UNCOMMON,
    borderColor = "black",
)

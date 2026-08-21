package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Greed reprint in Modern Horizons 2. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in the Legends (`leg`) `cards/` package; this file contributes only
 * per-printing presentation data.
 */
val GreedReprint = Printing(
    oracleId = "1ff62220-be95-4901-b8d8-812b9a1a1b0a",
    name = "Greed",
    setCode = "MH2",
    collectorNumber = "274",
    scryfallId = "851f8d1f-163c-4c4f-beee-431b64ec8a99",
    artist = "Izzy",
    imageUri = "https://cards.scryfall.io/normal/front/8/5/851f8d1f-163c-4c4f-beee-431b64ec8a99.jpg?1783926785",
    releaseDate = "2021-06-18",
    rarity = Rarity.UNCOMMON,
    borderColor = "black",
)

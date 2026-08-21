package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Counterspell reprint in Modern Horizons 2. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in the Alpha (`lea`) `cards/` package; this file contributes only per-printing
 * presentation data.
 */
val CounterspellReprint = Printing(
    oracleId = "cc187110-1148-4090-bbb8-e205694a39f5",
    name = "Counterspell",
    setCode = "MH2",
    collectorNumber = "267",
    scryfallId = "1920dae4-fb92-4f19-ae4b-eb3276b8dac7",
    artist = "Zack Stella",
    imageUri = "https://cards.scryfall.io/normal/front/1/9/1920dae4-fb92-4f19-ae4b-eb3276b8dac7.jpg?1783926788",
    releaseDate = "2021-06-18",
    rarity = Rarity.UNCOMMON,
    borderColor = "black",
)

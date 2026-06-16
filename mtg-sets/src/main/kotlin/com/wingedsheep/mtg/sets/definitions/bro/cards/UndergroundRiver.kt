package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Underground River reprint in BRO.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in ICE's `cards/` package
 * (the card's earliest real printing). This file contributes only the BRO-specific
 * presentation row, surfaced via the set's `printings`.
 */
val UndergroundRiverReprint = Printing(
    oracleId = "857febd9-cdd7-4f8e-a852-d88084b0cfbc",
    name = "Underground River",
    setCode = "BRO",
    collectorNumber = "267",
    artist = "Volkan Baǵa",
    imageUri = "https://cards.scryfall.io/normal/front/e/b/eb52820c-8660-4c4a-bb64-5b2fc580b6a3.jpg",
    releaseDate = "2022-11-18",
    rarity = Rarity.RARE,
)

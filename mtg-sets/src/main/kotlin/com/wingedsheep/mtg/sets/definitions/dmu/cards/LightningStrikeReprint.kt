package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lightning Strike reprint in DMU. Canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in Theros (THS); this file contributes only presentation data for the DMU printing.
 */
val LightningStrikeReprint = Printing(
    oracleId = "f34b9bc4-7bfe-47fd-ba23-4eeeb46026eb",
    name = "Lightning Strike",
    setCode = "DMU",
    collectorNumber = "137",
    scryfallId = "7d541125-bfb8-4f88-8bf3-ad7b6af7ad1d",
    artist = "Marta Nael",
    imageUri = "https://cards.scryfall.io/normal/front/7/d/7d541125-bfb8-4f88-8bf3-ad7b6af7ad1d.jpg?1673307449",
    releaseDate = "2022-09-09",
    rarity = Rarity.COMMON,
)

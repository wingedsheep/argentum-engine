package com.wingedsheep.mtg.sets.definitions.cmd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Buried Alive reprint in Commander. Canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in Weatherlight's `cards/` package; this file contributes only presentation data.
 */
val BuriedAliveReprint = Printing(
    oracleId = "8203c621-a1a0-4865-8c9a-0d4064c86107",
    name = "Buried Alive",
    setCode = "CMD",
    collectorNumber = "74",
    scryfallId = "1f82e318-ed20-40a1-b9b6-2aad10751e19",
    artist = "Greg Staples",
    imageUri = "https://cards.scryfall.io/normal/front/1/f/1f82e318-ed20-40a1-b9b6-2aad10751e19.jpg?1783941228",
    releaseDate = "2011-06-17",
    rarity = Rarity.UNCOMMON,
)

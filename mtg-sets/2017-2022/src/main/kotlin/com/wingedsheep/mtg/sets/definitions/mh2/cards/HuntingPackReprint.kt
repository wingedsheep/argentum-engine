package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Hunting Pack reprint in Modern Horizons 2. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in the Scourge (`scg`) `cards/` package; this file contributes only per-printing
 * presentation data.
 */
val HuntingPackReprint = Printing(
    oracleId = "d3e1848a-c236-4352-a7be-b0b4699af968",
    name = "Hunting Pack",
    setCode = "MH2",
    collectorNumber = "284",
    scryfallId = "8c9eb595-e8fa-4a5e-abca-d30613c0e28f",
    artist = "Lucas Graciano",
    imageUri = "https://cards.scryfall.io/normal/front/8/c/8c9eb595-e8fa-4a5e-abca-d30613c0e28f.jpg?1783926781",
    releaseDate = "2021-06-18",
    rarity = Rarity.UNCOMMON,
    borderColor = "black",
)

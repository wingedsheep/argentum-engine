package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lightning Strike reprint in DFT. Canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in Theros (THS); this file contributes only presentation data for the DFT printing.
 */
val LightningStrikeReprint = Printing(
    oracleId = "f34b9bc4-7bfe-47fd-ba23-4eeeb46026eb",
    name = "Lightning Strike",
    setCode = "DFT",
    collectorNumber = "136",
    scryfallId = "30077b49-b825-4dbb-a0c7-f3992f647df0",
    artist = "Steve Ellis",
    imageUri = "https://cards.scryfall.io/normal/front/3/0/30077b49-b825-4dbb-a0c7-f3992f647df0.jpg?1738356438",
    releaseDate = "2025-02-14",
    rarity = Rarity.COMMON,
)

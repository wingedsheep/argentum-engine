package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Pilfer reprint in Foundations (FDN). The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in DMU's `cards/` package; this file
 * contributes only presentation data.
 */
val PilferReprint = Printing(
    oracleId = "d13f0907-de39-4a90-940a-831740d7aa9b",
    name = "Pilfer",
    setCode = "FDN",
    collectorNumber = "181",
    scryfallId = "8c7c88b5-6d09-453b-b9c1-7dcbba8f1080",
    artist = "Pauline Voss",
    imageUri = "https://cards.scryfall.io/normal/front/8/c/8c7c88b5-6d09-453b-b9c1-7dcbba8f1080.jpg?1782689111",
    releaseDate = "2024-11-15",
    rarity = Rarity.COMMON,
)

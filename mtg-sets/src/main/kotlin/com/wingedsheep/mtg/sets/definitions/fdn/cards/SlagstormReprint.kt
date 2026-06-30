package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Slagstorm reprint in Foundations (FDN). The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in MBS's `cards/` package; this file
 * contributes only presentation data.
 */
val SlagstormReprint = Printing(
    oracleId = "2ed1879a-83b8-48df-a6d2-fe28e912b4c5",
    name = "Slagstorm",
    setCode = "FDN",
    collectorNumber = "207",
    scryfallId = "9db2e3a9-b90d-44cd-a2bd-eeb7dbe255b0",
    artist = "Dan Murayama Scott",
    imageUri = "https://cards.scryfall.io/normal/front/9/d/9db2e3a9-b90d-44cd-a2bd-eeb7dbe255b0.jpg?1782689087",
    releaseDate = "2024-11-15",
    rarity = Rarity.RARE,
)

package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Botanical Sanctum reprint in Outlaws of Thunder Junction. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Kaladesh (`kld`) `cards/` package;
 * this file contributes only per-printing presentation data.
 */
val BotanicalSanctumReprint = Printing(
    oracleId = "88f8f683-738e-48f3-afff-c8f73f1033a2",
    name = "Botanical Sanctum",
    setCode = "OTJ",
    collectorNumber = "267",
    scryfallId = "cc18d5f4-a56a-4f7d-9f56-ccc92cbfb7f7",
    artist = "Jorge Jacinto",
    imageUri = "https://cards.scryfall.io/normal/front/c/c/cc18d5f4-a56a-4f7d-9f56-ccc92cbfb7f7.jpg?1783911772",
    releaseDate = "2024-04-19",
    rarity = Rarity.RARE,
)

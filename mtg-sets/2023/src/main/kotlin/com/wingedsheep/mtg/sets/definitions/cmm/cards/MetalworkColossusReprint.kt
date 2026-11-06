package com.wingedsheep.mtg.sets.definitions.cmm.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Metalwork Colossus reprint in Commander Masters. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in the `kld` `cards/` package (the card's earliest real printing); this file contributes
 * only per-printing presentation data.
 */
val MetalworkColossusReprint = Printing(
    oracleId = "64ffe529-ba53-48ec-8af1-b046fbd1f0cc",
    name = "Metalwork Colossus",
    setCode = "CMM",
    collectorNumber = "960",
    scryfallId = "6f61e807-aa79-41b1-b785-24dddad28021",
    artist = "Jaime Jones",
    imageUri = "https://cards.scryfall.io/normal/front/6/f/6f61e807-aa79-41b1-b785-24dddad28021.jpg?1783915404",
    releaseDate = "2023-08-04",
    rarity = Rarity.RARE,
)

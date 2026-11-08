package com.wingedsheep.mtg.sets.definitions.brc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Metalwork Colossus reprint in The Brothers' War Commander. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in the `kld` `cards/` package (the card's earliest real printing); this file contributes
 * only per-printing presentation data.
 */
val MetalworkColossusReprint = Printing(
    oracleId = "64ffe529-ba53-48ec-8af1-b046fbd1f0cc",
    name = "Metalwork Colossus",
    setCode = "BRC",
    collectorNumber = "147",
    scryfallId = "df3e046e-ff1b-4bee-8611-229d0088f5a1",
    artist = "Jaime Jones",
    imageUri = "https://cards.scryfall.io/normal/front/d/f/df3e046e-ff1b-4bee-8611-229d0088f5a1.jpg?1783919664",
    releaseDate = "2022-11-18",
    rarity = Rarity.RARE,
)

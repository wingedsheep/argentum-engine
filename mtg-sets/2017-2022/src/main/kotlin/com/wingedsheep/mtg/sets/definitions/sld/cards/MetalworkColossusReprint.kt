package com.wingedsheep.mtg.sets.definitions.sld.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Metalwork Colossus reprints in Secret Lair Drop. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in the `kld` `cards/` package (the card's earliest real printing); this file contributes
 * only per-printing presentation data.
 */
val MetalworkColossusReprint = Printing(
    oracleId = "64ffe529-ba53-48ec-8af1-b046fbd1f0cc",
    name = "Metalwork Colossus",
    setCode = "SLD",
    collectorNumber = "377",
    scryfallId = "d482c454-425e-4d83-a8db-9b1c2d50403c",
    artist = "Chris Rahn, age 36",
    imageUri = "https://cards.scryfall.io/normal/front/d/4/d482c454-425e-4d83-a8db-9b1c2d50403c.jpg?1783925150",
    releaseDate = "2021-11-05",
    rarity = Rarity.RARE,
)

val MetalworkColossusVariantReprint = Printing(
    oracleId = "64ffe529-ba53-48ec-8af1-b046fbd1f0cc",
    name = "Metalwork Colossus",
    setCode = "SLD",
    collectorNumber = "378",
    scryfallId = "db91a198-49f9-4685-b03f-6f9513bddfd7",
    artist = "Hyan Tran, age 6",
    imageUri = "https://cards.scryfall.io/normal/front/d/b/db91a198-49f9-4685-b03f-6f9513bddfd7.jpg?1783925150",
    releaseDate = "2021-11-05",
    rarity = Rarity.RARE,
)

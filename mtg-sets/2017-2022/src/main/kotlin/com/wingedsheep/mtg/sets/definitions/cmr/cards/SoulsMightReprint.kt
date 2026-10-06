package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Soul's Might reprint in CMR. Canonical CardDefinition lives in Shards of Alara (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.ala.cards.SoulsMight`.
 */
val SoulsMightReprint = Printing(
    oracleId = "a3538a5e-3d08-4c61-bcb4-ef1f5e5b659d",
    name = "Soul's Might",
    setCode = "CMR",
    collectorNumber = "257",
    scryfallId = "dcd32c85-3cc5-4987-8156-4def6d0004d6",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/d/c/dcd32c85-3cc5-4987-8156-4def6d0004d6.jpg?1783928784",
    releaseDate = "2020-11-20",
    rarity = Rarity.COMMON,
)

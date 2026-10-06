package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Soul's Might reprint in J22. Canonical CardDefinition lives in Shards of Alara (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.ala.cards.SoulsMight`.
 */
val SoulsMightReprint = Printing(
    oracleId = "a3538a5e-3d08-4c61-bcb4-ef1f5e5b659d",
    name = "Soul's Might",
    setCode = "J22",
    collectorNumber = "731",
    scryfallId = "e2a912a5-8318-4cb3-aa62-6f826eaeb22b",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/e/2/e2a912a5-8318-4cb3-aa62-6f826eaeb22b.jpg?1783918832",
    releaseDate = "2022-12-02",
    rarity = Rarity.COMMON,
)

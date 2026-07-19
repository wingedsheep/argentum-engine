package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Giant Growth reprint in Marvel Super Heroes. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Alpha (`lea`) `cards/` package;
 * this file contributes only per-printing presentation data.
 */
val GiantGrowthReprint = Printing(
    oracleId = "5748ebf1-24e3-499d-ab7c-c2cebd462a24",
    name = "Giant Growth",
    setCode = "MSH",
    collectorNumber = "167",
    scryfallId = "fd1f95bf-48ea-455a-8a6c-0249b11c8900",
    artist = "Andreia Ugrai",
    imageUri = "https://cards.scryfall.io/normal/front/f/d/fd1f95bf-48ea-455a-8a6c-0249b11c8900.jpg?1783902918",
    releaseDate = "2026-06-26",
    rarity = Rarity.COMMON,
)

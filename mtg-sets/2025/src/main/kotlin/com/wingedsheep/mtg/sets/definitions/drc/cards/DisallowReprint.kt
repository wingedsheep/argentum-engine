package com.wingedsheep.mtg.sets.definitions.drc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Disallow reprint in DRC. Canonical CardDefinition lives in AER.
 */
val DisallowReprint = Printing(
    oracleId = "88b51e15-6630-4e14-a6b8-db0aa12e34ef",
    name = "Disallow",
    setCode = "DRC",
    collectorNumber = "74",
    scryfallId = "80f8aa2a-78e1-4717-a0bd-a435f4922668",
    artist = "Min Yum",
    imageUri = "https://cards.scryfall.io/normal/front/8/0/80f8aa2a-78e1-4717-a0bd-a435f4922668.jpg?1783907732",
    releaseDate = "2025-02-14",
    rarity = Rarity.RARE,
)

package com.wingedsheep.mtg.sets.definitions.trc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Disallow reprint in TRC. Canonical CardDefinition lives in AER.
 */
val DisallowReprint = Printing(
    oracleId = "88b51e15-6630-4e14-a6b8-db0aa12e34ef",
    name = "Disallow",
    setCode = "TRC",
    collectorNumber = "253",
    scryfallId = "9c54e2d0-eec6-4d85-9e44-0bd0500e79b4",
    artist = "Kristina WermoongRey",
    imageUri = "https://cards.scryfall.io/normal/front/9/c/9c54e2d0-eec6-4d85-9e44-0bd0500e79b4.jpg?1793101065",
    releaseDate = "2026-11-13",
    rarity = Rarity.RARE,
)

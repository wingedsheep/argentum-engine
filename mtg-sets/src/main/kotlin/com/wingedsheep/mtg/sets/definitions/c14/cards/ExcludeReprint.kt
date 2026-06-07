package com.wingedsheep.mtg.sets.definitions.c14.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Exclude reprint in C14. Canonical CardDefinition lives in its earliest set.
 */
val ExcludeReprint = Printing(
    oracleId = "a8c9f91a-b1e7-451d-b6db-ae865e2b853c",
    name = "Exclude",
    setCode = "C14",
    collectorNumber = "108",
    scryfallId = "970864e0-5488-4b6f-9316-3e3b4098770e",
    artist = "Greg Staples",
    imageUri = "https://cards.scryfall.io/normal/front/9/7/970864e0-5488-4b6f-9316-3e3b4098770e.jpg",
    releaseDate = "2014-11-07",
    rarity = Rarity.COMMON,
)

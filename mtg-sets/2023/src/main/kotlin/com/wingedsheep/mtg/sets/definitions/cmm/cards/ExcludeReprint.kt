package com.wingedsheep.mtg.sets.definitions.cmm.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Exclude reprint in CMM. Canonical CardDefinition lives in its earliest set.
 */
val ExcludeReprint = Printing(
    oracleId = "a8c9f91a-b1e7-451d-b6db-ae865e2b853c",
    name = "Exclude",
    setCode = "CMM",
    collectorNumber = "90",
    scryfallId = "014bc5a5-4483-42e9-9c14-1ad229b28eb7",
    artist = "Mark Romanoski",
    imageUri = "https://cards.scryfall.io/normal/front/0/1/014bc5a5-4483-42e9-9c14-1ad229b28eb7.jpg",
    releaseDate = "2023-08-04",
    rarity = Rarity.COMMON,
)

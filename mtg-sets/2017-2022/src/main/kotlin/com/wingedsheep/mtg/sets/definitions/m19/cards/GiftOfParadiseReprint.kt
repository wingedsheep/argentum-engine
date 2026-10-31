package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Gift of Paradise reprint in M19. Canonical CardDefinition lives in Amonkhet (its earliest real
 * printing), `com.wingedsheep.mtg.sets.definitions.akh.cards.GiftOfParadise`.
 */
val GiftOfParadiseM19Reprint = Printing(
    oracleId = "43a86d36-9653-4498-a533-8b30b919cdbf",
    name = "Gift of Paradise",
    setCode = "M19",
    collectorNumber = "184",
    scryfallId = "c0338075-5e84-4a37-957a-9cf23ac261ab",
    artist = "Ryan Pancoast",
    imageUri = "https://cards.scryfall.io/normal/front/c/0/c0338075-5e84-4a37-957a-9cf23ac261ab.jpg?1783934538",
    releaseDate = "2018-07-13",
    rarity = Rarity.UNCOMMON,
)

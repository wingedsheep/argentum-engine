package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Gift of Paradise reprint in CMR. Canonical CardDefinition lives in Amonkhet (its earliest real
 * printing), `com.wingedsheep.mtg.sets.definitions.akh.cards.GiftOfParadise`.
 */
val GiftOfParadiseCmrReprint = Printing(
    oracleId = "43a86d36-9653-4498-a533-8b30b919cdbf",
    name = "Gift of Paradise",
    setCode = "CMR",
    collectorNumber = "229",
    scryfallId = "00b361e3-8aaa-4194-8ae8-45bdb70c97bb",
    artist = "Ryan Pancoast",
    imageUri = "https://cards.scryfall.io/normal/front/0/0/00b361e3-8aaa-4194-8ae8-45bdb70c97bb.jpg?1783928793",
    releaseDate = "2020-11-20",
    rarity = Rarity.COMMON,
)

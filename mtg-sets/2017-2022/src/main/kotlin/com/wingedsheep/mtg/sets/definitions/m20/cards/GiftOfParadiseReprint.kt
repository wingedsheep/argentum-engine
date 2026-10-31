package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Gift of Paradise reprint in M20. Canonical CardDefinition lives in Amonkhet (its earliest real
 * printing), `com.wingedsheep.mtg.sets.definitions.akh.cards.GiftOfParadise`.
 */
val GiftOfParadiseReprint = Printing(
    oracleId = "43a86d36-9653-4498-a533-8b30b919cdbf",
    name = "Gift of Paradise",
    setCode = "M20",
    collectorNumber = "173",
    scryfallId = "d6b4e2b7-2687-4ad1-99e4-0f8411b5f50d",
    artist = "Ryan Pancoast",
    imageUri = "https://cards.scryfall.io/normal/front/d/6/d6b4e2b7-2687-4ad1-99e4-0f8411b5f50d.jpg?1783932966",
    releaseDate = "2019-07-12",
    rarity = Rarity.COMMON,
)

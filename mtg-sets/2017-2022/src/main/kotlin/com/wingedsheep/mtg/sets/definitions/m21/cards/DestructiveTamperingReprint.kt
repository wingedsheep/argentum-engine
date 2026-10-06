package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Destructive Tampering reprint in M21. Canonical CardDefinition lives in Aether Revolt (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.aer.cards.DestructiveTampering`.
 */
val DestructiveTamperingReprint = Printing(
    oracleId = "e6ae536a-95c0-495d-98b8-200b3564dd79",
    name = "Destructive Tampering",
    setCode = "M21",
    collectorNumber = "141",
    scryfallId = "bfe6a3a9-8d62-47c4-a78b-9baa9133a540",
    artist = "Titus Lunter",
    imageUri = "https://cards.scryfall.io/normal/front/b/f/bfe6a3a9-8d62-47c4-a78b-9baa9133a540.jpg?1783930692",
    releaseDate = "2020-07-03",
    rarity = Rarity.COMMON,
)

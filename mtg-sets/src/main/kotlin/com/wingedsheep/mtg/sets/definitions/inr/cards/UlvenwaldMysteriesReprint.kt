package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Ulvenwald Mysteries reprint in INR. Canonical CardDefinition lives in SOI's `cards/` package
 * (the card's earliest real printing); this file contributes only presentation data.
 */
val UlvenwaldMysteriesReprint = Printing(
    oracleId = "6588ea9a-658f-40d9-a64b-3a418ab81183",
    name = "Ulvenwald Mysteries",
    setCode = "INR",
    collectorNumber = "222",
    scryfallId = "8a3997f1-5b02-4ca5-a390-fedb5874b575",
    artist = "Nereida",
    imageUri = "https://cards.scryfall.io/normal/front/8/a/8a3997f1-5b02-4ca5-a390-fedb5874b575.jpg?1783908087",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)

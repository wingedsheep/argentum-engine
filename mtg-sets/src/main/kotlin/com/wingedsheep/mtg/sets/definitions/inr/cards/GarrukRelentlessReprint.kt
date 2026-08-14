package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Garruk Relentless reprint in INR. Canonical CardDefinition lives in ISD's `cards/` package
 * (the card's earliest real printing); this file contributes only presentation data.
 */
val GarrukRelentlessReprint = Printing(
    oracleId = "7cec9021-6f25-4fd8-b40e-adf4ffd3a7b8",
    name = "Garruk Relentless",
    setCode = "INR",
    collectorNumber = "197",
    scryfallId = "6897514f-e396-46d6-91e3-158366c741bb",
    artist = "Eric Deschamps",
    imageUri = "https://cards.scryfall.io/normal/front/6/8/6897514f-e396-46d6-91e3-158366c741bb.jpg?1783908104",
    backFaceImageUri = "https://cards.scryfall.io/normal/back/6/8/6897514f-e396-46d6-91e3-158366c741bb.jpg?1783908104",
    releaseDate = "2025-01-24",
    rarity = Rarity.MYTHIC,
)

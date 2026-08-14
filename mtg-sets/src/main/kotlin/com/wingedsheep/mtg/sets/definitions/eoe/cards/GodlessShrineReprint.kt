package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Godless Shrine reprint in Edge of Eternities. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Guildpact (`gpt`) `cards/` package
 * (the card's earliest real printing); this file contributes only per-printing presentation data.
 */
val GodlessShrineReprint = Printing(
    oracleId = "73864fcc-1bde-4bc0-831e-2b93e546e417",
    name = "Godless Shrine",
    setCode = "EOE",
    collectorNumber = "254",
    scryfallId = "8c542ea4-98c3-4c2d-9066-205ab7aa697a",
    artist = "Rob Rey",
    imageUri = "https://cards.scryfall.io/normal/front/8/c/8c542ea4-98c3-4c2d-9066-205ab7aa697a.jpg?1752947593",
    releaseDate = "2025-08-01",
    rarity = Rarity.RARE,
)

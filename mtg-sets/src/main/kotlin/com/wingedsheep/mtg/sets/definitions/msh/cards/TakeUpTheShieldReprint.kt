package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Take Up the Shield reprint in Marvel Super Heroes. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Dominaria United (`dmu`) `cards/`
 * package (earliest real-expansion printing); this file contributes only per-printing
 * presentation data.
 */
val TakeUpTheShieldReprint = Printing(
    oracleId = "e8d0eafe-540a-4b2e-a989-e98ff3c31105",
    name = "Take Up the Shield",
    setCode = "MSH",
    collectorNumber = "39",
    scryfallId = "5199d708-8b11-461d-9e91-3e8cd70906f7",
    artist = "Greg Staples",
    imageUri = "https://cards.scryfall.io/normal/front/5/1/5199d708-8b11-461d-9e91-3e8cd70906f7.jpg?1783902965",
    releaseDate = "2026-06-26",
    rarity = Rarity.COMMON,
)

package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Opportunistic Dragon reprint in TDC. The canonical CardDefinition lives in
 * Throne of Eldraine (`eld`), the card's earliest real printing; this file
 * contributes only per-printing presentation data.
 */
val OpportunisticDragonReprint = Printing(
    oracleId = "46f46bdd-52b8-436f-9a39-f5360a84b2cd",
    name = "Opportunistic Dragon",
    setCode = "TDC",
    collectorNumber = "228",
    scryfallId = "eac89f5e-6331-4f4a-b0b4-bc9c57f8b892",
    artist = "Chris Rahn",
    imageUri = "https://cards.scryfall.io/normal/front/e/a/eac89f5e-6331-4f4a-b0b4-bc9c57f8b892.jpg?1783907061",
    releaseDate = "2025-04-11",
    rarity = Rarity.RARE,
)

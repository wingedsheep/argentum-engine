package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Royal Assassin reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RoyalAssassinReprint = Printing(
    oracleId = "9ed6f28f-a3db-48c5-9ab0-b90a7fba5f57",
    name = "Royal Assassin",
    setCode = "LEB",
    collectorNumber = "124",
    artist = "Tom Wänerstrand",
    imageUri = "https://cards.scryfall.io/normal/front/b/6/b6e33c5e-6d99-4e7e-b611-4b271a47b4d2.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)

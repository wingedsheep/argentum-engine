package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Scryb Sprites reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ScrybSpritesReprint = Printing(
    oracleId = "a1f20695-6f08-4d5c-9fba-b0018bee298e",
    name = "Scryb Sprites",
    setCode = "LEB",
    collectorNumber = "216",
    artist = "Amy Weber",
    imageUri = "https://cards.scryfall.io/normal/front/f/a/fafe9639-e9d0-4aa2-8a16-f4ec24c140c0.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)

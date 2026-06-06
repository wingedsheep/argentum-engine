package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Concentrate reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ODY's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ConcentrateReprint = Printing(
    oracleId = "a3d287ec-1a39-4f91-acf1-7c1d2c00ed89",
    name = "Concentrate",
    setCode = "8ED",
    collectorNumber = "68",
    artist = "Glen Angus & Arnie Swekel",
    imageUri = "https://cards.scryfall.io/normal/front/e/d/ed69afaf-74cc-4b4f-8794-751477231cff.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)

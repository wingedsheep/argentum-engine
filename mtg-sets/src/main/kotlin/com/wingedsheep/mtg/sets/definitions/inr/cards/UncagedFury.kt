package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Uncaged Fury reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * SOI's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val UncagedFuryReprint = Printing(
    oracleId = "7aac51ef-ae03-4a9e-9a48-46223787f291",
    name = "Uncaged Fury",
    setCode = "INR",
    collectorNumber = "177",
    artist = "Jason Kang",
    imageUri = "https://cards.scryfall.io/normal/front/d/e/de8f4d5b-ea3c-4d01-b163-2fcfb1bcca8e.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)

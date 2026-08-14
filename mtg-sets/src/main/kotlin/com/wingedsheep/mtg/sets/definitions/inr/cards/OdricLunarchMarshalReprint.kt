package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Odric, Lunarch Marshal reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * SOI's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val OdricLunarchMarshalReprint = Printing(
    oracleId = "bad76170-c773-4be5-9457-20dc9f745cb4",
    name = "Odric, Lunarch Marshal",
    setCode = "INR",
    collectorNumber = "36",
    artist = "Chase Stone",
    imageUri = "https://cards.scryfall.io/normal/front/1/d/1db600b2-9b8b-4c21-8d8b-8033ec680a35.jpg?1783908176",
    releaseDate = "2025-01-24",
    rarity = Rarity.RARE,
)

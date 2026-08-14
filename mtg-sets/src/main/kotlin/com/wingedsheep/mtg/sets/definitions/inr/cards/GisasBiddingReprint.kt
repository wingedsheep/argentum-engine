package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Gisa's Bidding reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * SOI's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GisasBiddingReprint = Printing(
    oracleId = "0abc993b-18c7-4bb8-aa58-0d06a41cb48f",
    name = "Gisa's Bidding",
    setCode = "INR",
    collectorNumber = "111",
    artist = "Jason Felix",
    imageUri = "https://cards.scryfall.io/normal/front/5/a/5ad2fdbf-4a68-42e8-9c4f-7a075261ddf8.jpg?1783908142",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)

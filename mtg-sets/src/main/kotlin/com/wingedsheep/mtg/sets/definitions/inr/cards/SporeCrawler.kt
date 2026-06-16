package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Spore Crawler reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * VOW's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SporeCrawlerReprint = Printing(
    oracleId = "fa7c13c0-350b-4ed6-8c16-590d2b988184",
    name = "Spore Crawler",
    setCode = "INR",
    collectorNumber = "218",
    artist = "Nicholas Gregory",
    imageUri = "https://cards.scryfall.io/normal/front/a/2/a2a37c40-6d33-4e32-ab7b-4a7c2d10b757.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)

package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Drunau Corpse Trawler reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * SOI's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DrunauCorpseTrawlerReprint = Printing(
    oracleId = "870ca989-0e72-4e74-9570-57e129298f2e",
    name = "Drunau Corpse Trawler",
    setCode = "INR",
    collectorNumber = "63",
    artist = "Dave Kendall",
    imageUri = "https://cards.scryfall.io/normal/front/1/1/11b29a06-c1e1-4d81-ac50-906c94617abe.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)

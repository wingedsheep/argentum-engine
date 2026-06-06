package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Crossbow Infantry reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MMQ's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CrossbowInfantryReprint = Printing(
    oracleId = "dd99d791-3305-4195-ad12-96d958a28764",
    name = "Crossbow Infantry",
    setCode = "8ED",
    collectorNumber = "15",
    artist = "James Bernardin",
    imageUri = "https://cards.scryfall.io/normal/front/0/d/0d47101c-ead0-42a6-b37b-0e5d294959c9.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

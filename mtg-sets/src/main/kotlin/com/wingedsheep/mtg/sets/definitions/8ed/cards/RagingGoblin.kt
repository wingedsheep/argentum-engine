package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Raging Goblin reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RagingGoblinReprint = Printing(
    oracleId = "30997b43-fc13-41d3-8064-1ccc2cb6fd2b",
    name = "Raging Goblin",
    setCode = "8ED",
    collectorNumber = "212",
    artist = "Jeff Miracola",
    imageUri = "https://cards.scryfall.io/normal/front/1/6/165dc9fc-22b3-48ac-b446-5599af75917e.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

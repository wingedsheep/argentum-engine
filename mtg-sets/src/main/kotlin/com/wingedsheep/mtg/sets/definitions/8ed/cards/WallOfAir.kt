package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wall of Air reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WallOfAirReprint = Printing(
    oracleId = "b2d3da40-e2f7-4480-9da9-33019d6f4071",
    name = "Wall of Air",
    setCode = "8ED",
    collectorNumber = "113",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/5/1/517e4b04-19dd-42e6-9af4-4cc88a1bfcb4.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)

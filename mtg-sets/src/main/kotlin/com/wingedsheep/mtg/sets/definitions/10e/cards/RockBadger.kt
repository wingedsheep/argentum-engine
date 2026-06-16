package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Rock Badger reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MMQ's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RockBadgerReprint = Printing(
    oracleId = "1f5ac983-a7a5-4b9d-a25c-727a7de6c465",
    name = "Rock Badger",
    setCode = "10E",
    collectorNumber = "226",
    artist = "Heather Hudson",
    imageUri = "https://cards.scryfall.io/normal/front/4/9/49dc08b0-491f-470c-b920-2ca961d0d569.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

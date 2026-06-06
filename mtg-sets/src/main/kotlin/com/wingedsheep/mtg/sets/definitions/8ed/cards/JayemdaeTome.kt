package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Jayemdae Tome reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val JayemdaeTomeReprint = Printing(
    oracleId = "39ee576a-0803-4063-9c84-f2b537e4d44c",
    name = "Jayemdae Tome",
    setCode = "8ED",
    collectorNumber = "306",
    artist = "Donato Giancola",
    imageUri = "https://cards.scryfall.io/normal/front/c/7/c73b31b1-c347-4f1c-af4f-8ffbf9e8bf8a.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)

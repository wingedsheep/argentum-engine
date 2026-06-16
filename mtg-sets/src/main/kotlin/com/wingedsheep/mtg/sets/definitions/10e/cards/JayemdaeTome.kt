package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Jayemdae Tome reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val JayemdaeTomeReprint = Printing(
    oracleId = "39ee576a-0803-4063-9c84-f2b537e4d44c",
    name = "Jayemdae Tome",
    setCode = "10E",
    collectorNumber = "327",
    artist = "Donato Giancola",
    imageUri = "https://cards.scryfall.io/normal/front/a/8/a8f4736f-e19e-4e7b-9846-4d0806e46ce9.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)

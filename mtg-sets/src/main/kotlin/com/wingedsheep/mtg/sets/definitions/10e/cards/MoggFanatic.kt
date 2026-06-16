package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mogg Fanatic reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TMP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MoggFanaticReprint = Printing(
    oracleId = "7efd5d62-6da2-428a-98e3-5b842f668656",
    name = "Mogg Fanatic",
    setCode = "10E",
    collectorNumber = "219",
    artist = "Brom",
    imageUri = "https://cards.scryfall.io/normal/front/1/8/185949ab-55d0-439f-b25b-1f525e3eddf7.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

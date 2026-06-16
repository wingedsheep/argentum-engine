package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Overrun reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TMP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val OverrunReprint = Printing(
    oracleId = "204f9afe-c20b-4933-b5cd-aa572784762a",
    name = "Overrun",
    setCode = "10E",
    collectorNumber = "284",
    artist = "Carl Critchlow",
    imageUri = "https://cards.scryfall.io/normal/front/9/4/94bea6a3-a8eb-42c5-9224-89cdac7e8523.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Western Paladin reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * USG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WesternPaladinReprint = Printing(
    oracleId = "d2cbdbf9-9b08-419e-bb3e-482972d58e74",
    name = "Western Paladin",
    setCode = "8ED",
    collectorNumber = "173",
    artist = "Carl Critchlow",
    imageUri = "https://cards.scryfall.io/normal/front/4/f/4f829763-3dce-453f-a783-177ea085ee01.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)

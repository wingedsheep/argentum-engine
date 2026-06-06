package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Volcanic Hammer reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val VolcanicHammerReprint = Printing(
    oracleId = "98fa5a06-0553-40fd-999c-bc31c9b3f4db",
    name = "Volcanic Hammer",
    setCode = "8ED",
    collectorNumber = "231",
    artist = "Ben Thompson",
    imageUri = "https://cards.scryfall.io/normal/front/5/9/59c47e6a-71d8-4eba-ad05-0e6745f81600.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

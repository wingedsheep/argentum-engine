package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Vicious Hunger reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * NEM's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ViciousHungerReprint = Printing(
    oracleId = "38ea22cd-2c5d-4f66-a111-207aca4c67c3",
    name = "Vicious Hunger",
    setCode = "8ED",
    collectorNumber = "171",
    artist = "Massimiliano Frezzato",
    imageUri = "https://cards.scryfall.io/normal/front/e/d/edf7cf9e-cb8d-49ae-af5b-0da7f283e677.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

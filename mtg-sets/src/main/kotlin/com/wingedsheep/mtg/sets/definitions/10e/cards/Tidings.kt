package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Tidings reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * S99's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TidingsReprint = Printing(
    oracleId = "72897780-094d-4a21-8b1c-419a9defd2fb",
    name = "Tidings",
    setCode = "10E",
    collectorNumber = "116",
    artist = "Pete Venters",
    imageUri = "https://cards.scryfall.io/normal/front/3/9/39195bf4-896a-4a58-a7de-b9110216333b.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

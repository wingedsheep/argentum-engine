package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Leonin Scimitar reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MRD's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LeoninScimitarReprint = Printing(
    oracleId = "cde26d69-f3e7-4dd0-a53b-cd0ec812d717",
    name = "Leonin Scimitar",
    setCode = "10E",
    collectorNumber = "331",
    artist = "Doug Chaffee",
    imageUri = "https://cards.scryfall.io/normal/front/a/8/a8cceb7b-7166-491f-b471-8e0bc42f611e.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

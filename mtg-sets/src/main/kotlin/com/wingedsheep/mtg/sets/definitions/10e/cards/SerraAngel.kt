package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Serra Angel reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SerraAngelReprint = Printing(
    oracleId = "4b7ac066-e5c7-43e6-9e7e-2739b24a905d",
    name = "Serra Angel",
    setCode = "10E",
    collectorNumber = "39",
    artist = "Greg Staples",
    imageUri = "https://cards.scryfall.io/normal/front/6/5/658b2f11-7420-4fa7-98f0-4f4be2093355.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)

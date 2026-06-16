package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Earth Elemental reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val EarthElementalReprint = Printing(
    oracleId = "3c97c311-7ad5-47ec-b421-f6c3bfbda9fb",
    name = "Earth Elemental",
    setCode = "10E",
    collectorNumber = "199",
    artist = "Anthony S. Waters",
    imageUri = "https://cards.scryfall.io/normal/front/a/b/abdf4360-ba2f-464f-86f0-1620532e6a0a.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

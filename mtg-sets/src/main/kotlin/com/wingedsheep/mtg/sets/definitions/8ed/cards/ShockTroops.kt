package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Shock Troops reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MMQ's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ShockTroopsReprint = Printing(
    oracleId = "fdf2a8da-2933-44c5-b483-f035144da752",
    name = "Shock Troops",
    setCode = "8ED",
    collectorNumber = "223",
    artist = "Jeff Miracola",
    imageUri = "https://cards.scryfall.io/normal/front/4/9/49fe44af-5d8f-44ee-b767-2267917cf4bf.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

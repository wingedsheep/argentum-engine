package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Planar Portal reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * INV's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val PlanarPortalReprint = Printing(
    oracleId = "d4b10505-2b7a-4099-aacb-ff2359b24310",
    name = "Planar Portal",
    setCode = "8ED",
    collectorNumber = "311",
    artist = "Mark Tedin",
    imageUri = "https://cards.scryfall.io/normal/front/8/7/8743b2ce-0e18-42c9-aee4-f13197a9c481.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)

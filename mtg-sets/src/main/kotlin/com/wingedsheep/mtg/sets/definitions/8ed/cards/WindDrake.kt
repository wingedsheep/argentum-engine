package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wind Drake reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WindDrakeReprint = Printing(
    oracleId = "d6ffdaf0-ac08-4de9-bbce-2eab2f86bcca",
    name = "Wind Drake",
    setCode = "8ED",
    collectorNumber = "114",
    artist = "Tom Wänerstrand",
    imageUri = "https://cards.scryfall.io/normal/front/f/5/f5a57368-881f-4f1b-8fdd-09e76836227e.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

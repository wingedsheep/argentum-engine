package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Diabolic Tutor reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ODY's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DiabolicTutorReprint = Printing(
    oracleId = "14589b6b-1814-46f9-a364-83cc15dacac2",
    name = "Diabolic Tutor",
    setCode = "8ED",
    collectorNumber = "128",
    artist = "Rick Farrell",
    imageUri = "https://cards.scryfall.io/normal/front/7/c/7c7e0f9b-2c02-4e9a-ab86-270fad193de0.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)

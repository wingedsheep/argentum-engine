package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Diabolic Tutor reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ODY's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DiabolicTutorReprint = Printing(
    oracleId = "14589b6b-1814-46f9-a364-83cc15dacac2",
    name = "Diabolic Tutor",
    setCode = "10E",
    collectorNumber = "135",
    artist = "Greg Staples",
    imageUri = "https://cards.scryfall.io/normal/front/1/f/1fb98f0e-5fa0-497e-9061-85cf0b3a3eff.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

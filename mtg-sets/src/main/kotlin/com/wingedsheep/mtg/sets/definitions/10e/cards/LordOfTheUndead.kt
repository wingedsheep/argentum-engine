package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lord of the Undead reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * PLS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LordOfTheUndeadReprint = Printing(
    oracleId = "7714af0e-41d9-4609-967f-27233b46055f",
    name = "Lord of the Undead",
    setCode = "10E",
    collectorNumber = "155",
    artist = "Brom",
    imageUri = "https://cards.scryfall.io/normal/front/7/9/792e773b-5feb-407f-a162-f35d6e693cca.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)

package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Rod of Ruin reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RodOfRuinReprint = Printing(
    oracleId = "c29a04ab-4e14-45d8-a993-3fffd0e2f6eb",
    name = "Rod of Ruin",
    setCode = "10E",
    collectorNumber = "341",
    artist = "Mark Zug",
    imageUri = "https://cards.scryfall.io/normal/front/3/c/3c1f1431-b324-4915-89e8-2697cf755441.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

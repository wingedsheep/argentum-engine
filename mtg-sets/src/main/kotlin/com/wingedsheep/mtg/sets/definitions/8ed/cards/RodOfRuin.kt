package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Rod of Ruin reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RodOfRuinReprint = Printing(
    oracleId = "c29a04ab-4e14-45d8-a993-3fffd0e2f6eb",
    name = "Rod of Ruin",
    setCode = "8ED",
    collectorNumber = "312",
    artist = "David Martin",
    imageUri = "https://cards.scryfall.io/normal/front/1/d/1d652801-7f22-41b5-b19c-90efe5d8c0a8.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)

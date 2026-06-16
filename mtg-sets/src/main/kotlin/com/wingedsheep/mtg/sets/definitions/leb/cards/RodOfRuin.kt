package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Rod of Ruin reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RodOfRuinReprint = Printing(
    oracleId = "c29a04ab-4e14-45d8-a993-3fffd0e2f6eb",
    name = "Rod of Ruin",
    setCode = "LEB",
    collectorNumber = "269",
    artist = "Christopher Rush",
    imageUri = "https://cards.scryfall.io/normal/front/4/5/45810c0a-0a35-4bd4-ba66-5a45f8973fa4.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)

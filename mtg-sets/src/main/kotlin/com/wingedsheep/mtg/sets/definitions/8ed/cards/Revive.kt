package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Revive reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MMQ's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ReviveReprint = Printing(
    oracleId = "51991d28-137e-4690-a674-d852b904bd1a",
    name = "Revive",
    setCode = "8ED",
    collectorNumber = "276",
    artist = "Matthew D. Wilson",
    imageUri = "https://cards.scryfall.io/normal/front/0/d/0dbd99aa-af0a-489d-9be1-d3aaa0a1f5a4.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)

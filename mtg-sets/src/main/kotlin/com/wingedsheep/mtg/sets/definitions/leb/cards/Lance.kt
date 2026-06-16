package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lance reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LanceReprint = Printing(
    oracleId = "5960dd01-6797-4c73-b48a-f637b9c288cc",
    name = "Lance",
    setCode = "LEB",
    collectorNumber = "28",
    artist = "Rob Alexander",
    imageUri = "https://cards.scryfall.io/normal/front/a/7/a7aa3a93-3765-49f0-8ff2-b6843509c34a.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)

package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Phantom Warrior reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val PhantomWarriorReprint = Printing(
    oracleId = "23745133-e5e2-4ce3-b94a-73d0d3d8a013",
    name = "Phantom Warrior",
    setCode = "8ED",
    collectorNumber = "93",
    artist = "Greg Staples",
    imageUri = "https://cards.scryfall.io/normal/front/b/e/bef3f3aa-b54d-433f-9798-3b0fd1755094.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)

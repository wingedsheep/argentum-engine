package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Icy Manipulator reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val IcyManipulatorReprint = Printing(
    oracleId = "3608f1f7-8dc5-4dd1-ae91-c830e1de9529",
    name = "Icy Manipulator",
    setCode = "10E",
    collectorNumber = "326",
    artist = "Matt Cavotta",
    imageUri = "https://cards.scryfall.io/normal/front/3/c/3c67344c-2066-4e19-88a2-52b272d7d216.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Icy Manipulator reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val IcyManipulatorReprint = Printing(
    oracleId = "3608f1f7-8dc5-4dd1-ae91-c830e1de9529",
    name = "Icy Manipulator",
    setCode = "LEB",
    collectorNumber = "249",
    artist = "Douglas Shuler",
    imageUri = "https://cards.scryfall.io/normal/front/d/2/d27608e7-6539-4813-95b6-d8847cdc6a12.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)

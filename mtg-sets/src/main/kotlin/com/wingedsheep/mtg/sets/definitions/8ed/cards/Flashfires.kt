package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Flashfires reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FlashfiresReprint = Printing(
    oracleId = "c281f436-8c77-48f7-b31c-d40cd7f9ed6a",
    name = "Flashfires",
    setCode = "8ED",
    collectorNumber = "186",
    artist = "Randy Gallegos",
    imageUri = "https://cards.scryfall.io/normal/front/8/2/827a2e80-68ef-4d13-ba89-a5ea33d70cc4.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)

package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Phyrexian Hulk reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TMP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val PhyrexianHulkReprint = Printing(
    oracleId = "a16509da-ad46-4ee3-86f3-06c521c23481",
    name = "Phyrexian Hulk",
    setCode = "8ED",
    collectorNumber = "310",
    artist = "Matthew D. Wilson",
    imageUri = "https://cards.scryfall.io/normal/front/b/a/ba64af09-2af7-4630-a3c4-e1aa90f2a9fb.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)

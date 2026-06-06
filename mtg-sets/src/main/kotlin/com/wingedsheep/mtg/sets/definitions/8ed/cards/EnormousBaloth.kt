package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Enormous Baloth reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LGN's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val EnormousBalothReprint = Printing(
    oracleId = "6189cd17-61aa-420a-ba53-5ddaf2bbc2ba",
    name = "Enormous Baloth",
    setCode = "8ED",
    collectorNumber = "S6",
    artist = "Mark Tedin",
    imageUri = "https://cards.scryfall.io/normal/front/a/9/a9256cc2-d4b5-4103-bda8-2d9c98c4fd7d.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)

package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Enormous Baloth reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LGN's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val EnormousBalothReprint = Printing(
    oracleId = "6189cd17-61aa-420a-ba53-5ddaf2bbc2ba",
    name = "Enormous Baloth",
    setCode = "10E",
    collectorNumber = "263",
    artist = "Mark Tedin",
    imageUri = "https://cards.scryfall.io/normal/front/f/6/f698125f-3961-4295-8dcf-3227ec9f4694.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

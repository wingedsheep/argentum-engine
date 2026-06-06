package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Angelic Page reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * USG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AngelicPageReprint = Printing(
    oracleId = "f7f76abc-8656-4e97-9b7f-7f89ca3e6d93",
    name = "Angelic Page",
    setCode = "8ED",
    collectorNumber = "2",
    artist = "Marc Fishman",
    imageUri = "https://cards.scryfall.io/normal/front/c/7/c70a624f-3d5a-4519-8711-bd163da305c7.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

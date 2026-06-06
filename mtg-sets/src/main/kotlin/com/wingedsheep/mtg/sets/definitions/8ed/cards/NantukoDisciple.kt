package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Nantuko Disciple reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ODY's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val NantukoDiscipleReprint = Printing(
    oracleId = "230be0f8-03a4-4452-84c1-ea14cfe49737",
    name = "Nantuko Disciple",
    setCode = "8ED",
    collectorNumber = "268",
    artist = "Justin Sweet",
    imageUri = "https://cards.scryfall.io/normal/front/3/2/32bd6695-ee30-4ab1-a732-26b073c2fec6.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

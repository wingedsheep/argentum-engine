package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Aven Cloudchaser reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ODY's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AvenCloudchaserReprint = Printing(
    oracleId = "48bda7dd-d023-41e8-8c28-e0cfda0d07ca",
    name = "Aven Cloudchaser",
    setCode = "8ED",
    collectorNumber = "5",
    artist = "Justin Sweet",
    imageUri = "https://cards.scryfall.io/normal/front/0/3/03872618-1428-48c9-8f6d-8edc53560df2.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

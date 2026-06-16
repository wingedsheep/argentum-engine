package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Aven Cloudchaser reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ODY's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AvenCloudchaserReprint = Printing(
    oracleId = "48bda7dd-d023-41e8-8c28-e0cfda0d07ca",
    name = "Aven Cloudchaser",
    setCode = "10E",
    collectorNumber = "7",
    artist = "Justin Sweet",
    imageUri = "https://cards.scryfall.io/normal/front/4/0/407110e9-19af-4ff5-97b2-c03225031a73.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

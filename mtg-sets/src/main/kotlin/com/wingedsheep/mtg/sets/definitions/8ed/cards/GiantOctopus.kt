package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Giant Octopus reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GiantOctopusReprint = Printing(
    oracleId = "c181d2a4-5959-4409-9bd3-ecedf8ec9516",
    name = "Giant Octopus",
    setCode = "8ED",
    collectorNumber = "S3",
    artist = "Heather Hudson",
    imageUri = "https://cards.scryfall.io/normal/front/e/5/e5131c74-a81b-4927-a1b9-98d2c714ab71.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

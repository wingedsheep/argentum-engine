package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Skull of Orm reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * DRK's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SkullOfOrmReprint = Printing(
    oracleId = "67601399-089b-44fe-82db-c319729c75bf",
    name = "Skull of Orm",
    setCode = "8ED",
    collectorNumber = "313",
    artist = "Daren Bader",
    imageUri = "https://cards.scryfall.io/normal/front/c/c/ccc3a7b4-ff44-4909-ab8d-35ba5d187815.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)

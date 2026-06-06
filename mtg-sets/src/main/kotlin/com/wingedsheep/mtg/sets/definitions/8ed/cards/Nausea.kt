package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Nausea reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * EXO's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val NauseaReprint = Printing(
    oracleId = "2b8abae2-dd15-41ee-81ae-6a463723b43e",
    name = "Nausea",
    setCode = "8ED",
    collectorNumber = "148",
    artist = "James Bernardin",
    imageUri = "https://cards.scryfall.io/normal/front/3/5/358efb1e-0366-4943-9a25-6a356e48773c.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

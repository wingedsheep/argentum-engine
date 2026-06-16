package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Smash reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * APC's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SmashReprint = Printing(
    oracleId = "1602f8c7-fe00-4bc9-b4fc-b26b7223ac75",
    name = "Smash",
    setCode = "10E",
    collectorNumber = "235",
    artist = "Paolo Parente",
    imageUri = "https://cards.scryfall.io/normal/front/4/0/40c39852-efd3-4a25-b33b-3c13dad96e7d.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

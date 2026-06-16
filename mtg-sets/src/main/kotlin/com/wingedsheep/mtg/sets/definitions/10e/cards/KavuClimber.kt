package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Kavu Climber reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * INV's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val KavuClimberReprint = Printing(
    oracleId = "91320afd-1d42-4cb5-ae40-4eed2fa91dfe",
    name = "Kavu Climber",
    setCode = "10E",
    collectorNumber = "273",
    artist = "Rob Alexander",
    imageUri = "https://cards.scryfall.io/normal/front/b/3/b343c2ba-e646-4614-aace-9f8772974069.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

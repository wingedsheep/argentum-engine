package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lava Axe reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LavaAxeReprint = Printing(
    oracleId = "387b6b07-a283-412d-94c3-f7f1dc76e858",
    name = "Lava Axe",
    setCode = "10E",
    collectorNumber = "215",
    artist = "Brian Snõddy",
    imageUri = "https://cards.scryfall.io/normal/front/1/a/1a28a286-3ea0-4760-bcc4-c7e8299c298e.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

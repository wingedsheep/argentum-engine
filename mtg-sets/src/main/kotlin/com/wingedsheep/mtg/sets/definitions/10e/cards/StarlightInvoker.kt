package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Starlight Invoker reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LGN's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val StarlightInvokerReprint = Printing(
    oracleId = "4d27a90b-dae4-48a0-9eb2-e199ff548867",
    name = "Starlight Invoker",
    setCode = "10E",
    collectorNumber = "47",
    artist = "Glen Angus",
    imageUri = "https://cards.scryfall.io/normal/front/d/8/d8d2139e-76ea-49e2-ac43-6d9cc51ca09f.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

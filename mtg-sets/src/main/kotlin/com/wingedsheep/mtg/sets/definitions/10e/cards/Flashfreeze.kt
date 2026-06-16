package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Flashfreeze reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * CSP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FlashfreezeReprint = Printing(
    oracleId = "eaf98e03-729b-4145-b2af-c910c415c15d",
    name = "Flashfreeze",
    setCode = "10E",
    collectorNumber = "84",
    artist = "Brian Despain",
    imageUri = "https://cards.scryfall.io/normal/front/6/d/6d3e5fd8-2542-4fc9-a806-d9af287dd734.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

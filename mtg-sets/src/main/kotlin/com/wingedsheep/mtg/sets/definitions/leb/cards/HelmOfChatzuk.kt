package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Helm of Chatzuk reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HelmOfChatzukReprint = Printing(
    oracleId = "948e3bb7-8265-4e95-acd1-a0c4f22441df",
    name = "Helm of Chatzuk",
    setCode = "LEB",
    collectorNumber = "247",
    artist = "Mark Tedin",
    imageUri = "https://cards.scryfall.io/normal/front/5/5/559d3329-9053-4301-b867-1b49c248fe31.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)

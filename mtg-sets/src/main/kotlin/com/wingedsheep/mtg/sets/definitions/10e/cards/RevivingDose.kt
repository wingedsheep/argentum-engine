package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Reviving Dose reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * INV's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RevivingDoseReprint = Printing(
    oracleId = "ad470afc-d1a5-4e64-8192-fb6308ed9bfe",
    name = "Reviving Dose",
    setCode = "10E",
    collectorNumber = "34",
    artist = "D. Alexander Gregory",
    imageUri = "https://cards.scryfall.io/normal/front/1/5/15b3dd79-ac4c-4ffc-9442-1efa0082f60f.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Elvish Champion reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * INV's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ElvishChampionReprint = Printing(
    oracleId = "7e40f37c-9a0c-40e0-b195-7ea94b12f798",
    name = "Elvish Champion",
    setCode = "10E",
    collectorNumber = "261",
    artist = "D. Alexander Gregory",
    imageUri = "https://cards.scryfall.io/normal/front/a/d/ad42a3fd-21ae-49cf-b11d-3700835f2f96.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)

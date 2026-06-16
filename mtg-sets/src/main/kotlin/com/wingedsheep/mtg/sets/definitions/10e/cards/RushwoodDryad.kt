package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Rushwood Dryad reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MMQ's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RushwoodDryadReprint = Printing(
    oracleId = "bb1963f9-261c-4d3f-a401-90aaced77e7f",
    name = "Rushwood Dryad",
    setCode = "10E",
    collectorNumber = "294",
    artist = "Todd Lockwood",
    imageUri = "https://cards.scryfall.io/normal/front/3/e/3ed23582-3260-44c6-9884-904d294442ad.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

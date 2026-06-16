package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Primal Rage reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * STH's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val PrimalRageReprint = Printing(
    oracleId = "d6464ee4-23fc-4d68-bbda-3b53772015d1",
    name = "Primal Rage",
    setCode = "10E",
    collectorNumber = "286",
    artist = "Brian Snõddy",
    imageUri = "https://cards.scryfall.io/normal/front/e/b/ebe3b738-703d-465c-bc76-2b66f1e0aff2.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

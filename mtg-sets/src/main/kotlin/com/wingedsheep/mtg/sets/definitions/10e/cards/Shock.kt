package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Shock reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * STH's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ShockReprint = Printing(
    oracleId = "a9d288b8-cdc1-4e55-a0c9-d6edfc95e65d",
    name = "Shock",
    setCode = "10E",
    collectorNumber = "232",
    artist = "Jon Foster",
    imageUri = "https://cards.scryfall.io/normal/front/3/3/334ad39a-4088-4530-8f3c-d34e7cc99fae.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

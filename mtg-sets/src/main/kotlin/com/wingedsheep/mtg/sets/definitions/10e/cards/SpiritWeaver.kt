package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Spirit Weaver reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * INV's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SpiritWeaverReprint = Printing(
    oracleId = "4e6e28f1-111b-4bac-b814-487f6c206485",
    name = "Spirit Weaver",
    setCode = "10E",
    collectorNumber = "46",
    artist = "Matthew D. Wilson",
    imageUri = "https://cards.scryfall.io/normal/front/2/8/286e436d-5c46-4a39-b773-99548befb9fa.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

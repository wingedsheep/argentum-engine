package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Recollect reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * RAV's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RecollectReprint = Printing(
    oracleId = "5b38fafb-3999-46dc-928b-1677625c1943",
    name = "Recollect",
    setCode = "10E",
    collectorNumber = "289",
    artist = "Pete Venters",
    imageUri = "https://cards.scryfall.io/normal/front/3/7/37eb6ca3-0d7f-45ba-99d2-283d517f1463.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

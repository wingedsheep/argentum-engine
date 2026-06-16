package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Loxodon Mystic reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * DST's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LoxodonMysticReprint = Printing(
    oracleId = "f3e22884-a9ca-4479-b400-df1c1d2fd153",
    name = "Loxodon Mystic",
    setCode = "10E",
    collectorNumber = "26",
    artist = "Randy Gallegos",
    imageUri = "https://cards.scryfall.io/normal/front/8/3/83bf030b-4e20-49b0-837f-935f96b5d1fe.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

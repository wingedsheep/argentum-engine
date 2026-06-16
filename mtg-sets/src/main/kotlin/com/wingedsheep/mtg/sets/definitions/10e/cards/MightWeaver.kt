package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Might Weaver reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * INV's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MightWeaverReprint = Printing(
    oracleId = "c2cfbc1a-ae73-409c-b73a-5c8625365905",
    name = "Might Weaver",
    setCode = "10E",
    collectorNumber = "278",
    artist = "Larry Elmore",
    imageUri = "https://cards.scryfall.io/normal/front/d/b/dbd09923-4a4b-4ac6-a487-6e00d17183cb.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

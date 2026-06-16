package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sky Weaver reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * INV's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SkyWeaverReprint = Printing(
    oracleId = "25933e8b-fa48-4152-9eae-388aef10ad37",
    name = "Sky Weaver",
    setCode = "10E",
    collectorNumber = "109",
    artist = "Christopher Moeller",
    imageUri = "https://cards.scryfall.io/normal/front/5/0/5095e0e4-d077-46a5-850c-68399fd16da6.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

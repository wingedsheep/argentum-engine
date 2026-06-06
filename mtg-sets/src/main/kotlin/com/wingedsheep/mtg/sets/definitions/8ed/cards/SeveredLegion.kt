package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Severed Legion reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ONS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SeveredLegionReprint = Printing(
    oracleId = "34a8a658-a5f6-406f-99d8-c32ea2e26202",
    name = "Severed Legion",
    setCode = "8ED",
    collectorNumber = "163",
    artist = "Dany Orizio",
    imageUri = "https://cards.scryfall.io/normal/front/7/1/710a3e43-a56a-4a41-86b3-72d48d324bf3.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

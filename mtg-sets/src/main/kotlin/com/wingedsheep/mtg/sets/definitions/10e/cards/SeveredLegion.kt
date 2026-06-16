package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Severed Legion reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ONS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SeveredLegionReprint = Printing(
    oracleId = "34a8a658-a5f6-406f-99d8-c32ea2e26202",
    name = "Severed Legion",
    setCode = "10E",
    collectorNumber = "177",
    artist = "Dany Orizio",
    imageUri = "https://cards.scryfall.io/normal/front/8/2/82633f38-5af1-429e-8c9d-db536af85309.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

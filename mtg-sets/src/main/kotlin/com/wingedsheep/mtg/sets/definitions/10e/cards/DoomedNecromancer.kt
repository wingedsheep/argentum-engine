package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Doomed Necromancer reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ONS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DoomedNecromancerReprint = Printing(
    oracleId = "155422a0-a0cd-4399-8ed9-fa68ac2c80a6",
    name = "Doomed Necromancer",
    setCode = "10E",
    collectorNumber = "137",
    artist = "Volkan Baǵa",
    imageUri = "https://cards.scryfall.io/normal/front/3/8/384ff8ca-068d-4115-9139-df00b63e2912.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)

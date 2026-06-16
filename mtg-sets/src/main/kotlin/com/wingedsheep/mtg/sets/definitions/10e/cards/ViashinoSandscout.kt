package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Viashino Sandscout reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ULG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ViashinoSandscoutReprint = Printing(
    oracleId = "f8d5a65c-382b-4277-9c08-5dec9e395b02",
    name = "Viashino Sandscout",
    setCode = "10E",
    collectorNumber = "246",
    artist = "Scott M. Fischer",
    imageUri = "https://cards.scryfall.io/normal/front/1/0/102f139d-1baf-4ca6-a940-70b7efdc29f2.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

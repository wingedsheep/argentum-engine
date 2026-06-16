package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mass of Ghouls reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * FUT's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MassOfGhoulsReprint = Printing(
    oracleId = "29646827-7b69-48c5-8273-4e66076e2384",
    name = "Mass of Ghouls",
    setCode = "10E",
    collectorNumber = "156",
    artist = "Lucio Parrillo",
    imageUri = "https://cards.scryfall.io/normal/front/4/f/4fe5e3ea-fcca-42c2-b3af-bfcd3a81dfff.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

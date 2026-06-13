package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Drudge Skeletons reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DrudgeSkeletonsReprint = Printing(
    oracleId = "c180ed02-07f4-4538-9bea-8c249234a8e2",
    name = "Drudge Skeletons",
    setCode = "LEB",
    collectorNumber = "107",
    artist = "Sandra Everingham",
    imageUri = "https://cards.scryfall.io/normal/front/b/1/b1f3a1b9-d192-49d9-87bb-ca50e99edbd1.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)

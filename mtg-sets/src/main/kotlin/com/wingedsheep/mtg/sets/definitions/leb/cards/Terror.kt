package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Terror reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TerrorReprint = Printing(
    oracleId = "b81f041d-98db-4408-9472-c483e4a502bc",
    name = "Terror",
    setCode = "LEB",
    collectorNumber = "131",
    artist = "Ron Spencer",
    imageUri = "https://cards.scryfall.io/normal/front/5/8/58d8598b-35e5-414f-aee0-52137236f642.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)

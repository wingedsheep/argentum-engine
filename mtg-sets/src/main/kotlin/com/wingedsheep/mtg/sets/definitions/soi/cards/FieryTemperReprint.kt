package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fiery Temper reprint in SOI.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TOR's `cards/` package (the card's earliest real printing). This file contributes only
 * the SOI-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FieryTemperReprint = Printing(
    oracleId = "f07bd49d-8e71-4d56-be2a-638514011318",
    name = "Fiery Temper",
    setCode = "SOI",
    collectorNumber = "156",
    artist = "Johannes Voss",
    imageUri = "https://cards.scryfall.io/normal/front/6/1/61caf82d-e077-4931-a6ad-09fa7f04b36f.jpg?1783937754",
    releaseDate = "2016-04-08",
    rarity = Rarity.COMMON,
)

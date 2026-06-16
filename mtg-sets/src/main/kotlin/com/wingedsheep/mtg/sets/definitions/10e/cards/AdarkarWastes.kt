package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Adarkar Wastes reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ICE's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AdarkarWastesReprint = Printing(
    oracleId = "d5ad26cc-2bdb-46b7-b8bf-dd099d5fa09b",
    name = "Adarkar Wastes",
    setCode = "10E",
    collectorNumber = "347",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/f/3/f31cf014-7ac9-428b-9ce9-8ba5ebfdd252.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)

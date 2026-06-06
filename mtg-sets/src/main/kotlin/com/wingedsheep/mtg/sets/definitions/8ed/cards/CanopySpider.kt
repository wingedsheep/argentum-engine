package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Canopy Spider reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TMP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CanopySpiderReprint = Printing(
    oracleId = "37f3733e-cc4e-4d84-b29b-d474f6e254a2",
    name = "Canopy Spider",
    setCode = "8ED",
    collectorNumber = "236",
    artist = "Christopher Rush",
    imageUri = "https://cards.scryfall.io/normal/front/d/e/def682d0-6091-49c8-ac46-da0884b8cdb3.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

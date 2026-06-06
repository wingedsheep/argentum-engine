package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mind Rot reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MindRotReprint = Printing(
    oracleId = "ad44cf74-b717-48fb-9fa2-77512024d76a",
    name = "Mind Rot",
    setCode = "8ED",
    collectorNumber = "144",
    artist = "Steve Luke",
    imageUri = "https://cards.scryfall.io/normal/front/0/b/0b780692-9c74-4283-a1ca-63f991dcc2c3.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

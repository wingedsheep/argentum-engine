package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Shock reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * STH's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ShockReprint = Printing(
    oracleId = "a9d288b8-cdc1-4e55-a0c9-d6edfc95e65d",
    name = "Shock",
    setCode = "8ED",
    collectorNumber = "222",
    artist = "Randy Gallegos",
    imageUri = "https://cards.scryfall.io/normal/front/d/a/daef5f10-ccb5-4304-b04f-b52a7d2f4158.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

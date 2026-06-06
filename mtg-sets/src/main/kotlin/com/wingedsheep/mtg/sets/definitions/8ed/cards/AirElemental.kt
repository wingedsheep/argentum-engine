package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Air Elemental reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AirElementalReprint = Printing(
    oracleId = "7744bae4-a8b7-44a5-9b4c-0048ad4cc448",
    name = "Air Elemental",
    setCode = "8ED",
    collectorNumber = "59",
    artist = "Wayne England",
    imageUri = "https://cards.scryfall.io/normal/front/3/b/3b15648c-1d88-42ec-b2f7-aab9a8c256a2.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)

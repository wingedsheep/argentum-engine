package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lightning Elemental reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TMP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LightningElementalReprint = Printing(
    oracleId = "58aee5cb-7b88-446e-ab10-9f83c10d7227",
    name = "Lightning Elemental",
    setCode = "8ED",
    collectorNumber = "201",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/0/0/00cf2252-5ead-47df-8150-06c81dc47584.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

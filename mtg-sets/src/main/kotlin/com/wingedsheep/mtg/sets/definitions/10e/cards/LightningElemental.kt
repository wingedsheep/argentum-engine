package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lightning Elemental reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TMP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LightningElementalReprint = Printing(
    oracleId = "58aee5cb-7b88-446e-ab10-9f83c10d7227",
    name = "Lightning Elemental",
    setCode = "10E",
    collectorNumber = "217",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/9/a/9a557c16-2b58-45c4-9e97-cf6047a83d17.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

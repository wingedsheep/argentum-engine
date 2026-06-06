package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Angel of Mercy reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * P02's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AngelOfMercyReprint = Printing(
    oracleId = "a2daaf32-dbfe-4618-892e-0da24f63a44a",
    name = "Angel of Mercy",
    setCode = "8ED",
    collectorNumber = "1",
    artist = "Melissa A. Benson",
    imageUri = "https://cards.scryfall.io/normal/front/5/5/552cc36c-4d02-44ee-b36a-f58ff14c6f3d.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)

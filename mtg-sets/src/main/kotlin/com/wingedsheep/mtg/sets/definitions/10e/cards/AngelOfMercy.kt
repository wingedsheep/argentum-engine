package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Angel of Mercy reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * P02's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AngelOfMercyReprint = Printing(
    oracleId = "a2daaf32-dbfe-4618-892e-0da24f63a44a",
    name = "Angel of Mercy",
    setCode = "10E",
    collectorNumber = "2",
    artist = "Volkan Baǵa",
    imageUri = "https://cards.scryfall.io/normal/front/8/f/8f7980d4-da43-4d6d-ad16-14b8a34ae91d.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Angelic Wall reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * P02's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AngelicWallReprint = Printing(
    oracleId = "4502b24f-604b-4e36-9168-31c1a1ab4dab",
    name = "Angelic Wall",
    setCode = "10E",
    collectorNumber = "5",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/6/f/6fa1f42f-1729-46ea-99c6-015d66c627d4.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

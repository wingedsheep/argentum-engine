package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Invisibility reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val InvisibilityReprint = Printing(
    oracleId = "de26b0c6-dfb7-45a8-9d7f-f8d45522d675",
    name = "Invisibility",
    setCode = "8ED",
    collectorNumber = "87",
    artist = "Pete Venters",
    imageUri = "https://cards.scryfall.io/normal/front/b/3/b3d06b2b-9979-49b2-900f-795d1d5945e4.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)

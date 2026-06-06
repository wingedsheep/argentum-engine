package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Rushwood Dryad reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MMQ's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RushwoodDryadReprint = Printing(
    oracleId = "bb1963f9-261c-4d3f-a401-90aaced77e7f",
    name = "Rushwood Dryad",
    setCode = "8ED",
    collectorNumber = "278",
    artist = "Todd Lockwood",
    imageUri = "https://cards.scryfall.io/normal/front/4/d/4d62152d-463b-464d-b3c2-8b404ee1f80e.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

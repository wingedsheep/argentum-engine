package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bog Imp reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * DRK's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BogImpReprint = Printing(
    oracleId = "45b94e3c-a905-435b-aee5-bec9239fd24c",
    name = "Bog Imp",
    setCode = "8ED",
    collectorNumber = "119",
    artist = "Carl Critchlow",
    imageUri = "https://cards.scryfall.io/normal/front/9/4/94903771-d0d0-49dd-b28a-438ef4bdf416.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

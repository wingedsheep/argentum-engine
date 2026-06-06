package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Foratog reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MIR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ForatogReprint = Printing(
    oracleId = "77f257b9-2047-40a3-85c2-00ad17deeb08",
    name = "Foratog",
    setCode = "8ED",
    collectorNumber = "249",
    artist = "Mark Poole",
    imageUri = "https://cards.scryfall.io/normal/front/5/d/5d621fad-3a50-4910-a302-23ae9e77ebde.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)

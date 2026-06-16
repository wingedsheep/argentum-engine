package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Flamewave Invoker reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LGN's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FlamewaveInvokerReprint = Printing(
    oracleId = "ee17706a-edc0-4301-8330-1da539679c7a",
    name = "Flamewave Invoker",
    setCode = "10E",
    collectorNumber = "202",
    artist = "Dave Dorman",
    imageUri = "https://cards.scryfall.io/normal/front/9/d/9dad8247-1970-4dfb-aad5-d683160ab1b9.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

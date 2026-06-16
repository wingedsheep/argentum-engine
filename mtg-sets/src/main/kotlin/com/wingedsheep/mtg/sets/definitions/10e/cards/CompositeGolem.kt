package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Composite Golem reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * 5DN's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CompositeGolemReprint = Printing(
    oracleId = "784970de-ef8c-4672-b5d0-24a4b0a978a3",
    name = "Composite Golem",
    setCode = "10E",
    collectorNumber = "318",
    artist = "Mark Tedin",
    imageUri = "https://cards.scryfall.io/normal/front/a/a/aafd4637-737d-4913-80e3-ab4bf3428680.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

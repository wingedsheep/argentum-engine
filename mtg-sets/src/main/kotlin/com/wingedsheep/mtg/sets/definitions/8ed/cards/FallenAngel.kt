package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fallen Angel reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FallenAngelReprint = Printing(
    oracleId = "1869a326-8b48-4331-b969-5ce83cf4ff8a",
    name = "Fallen Angel",
    setCode = "8ED",
    collectorNumber = "133",
    artist = "Matthew D. Wilson",
    imageUri = "https://cards.scryfall.io/normal/front/2/c/2c574bf0-0fad-4cde-a75e-88d899a890ee.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)

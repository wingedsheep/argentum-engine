package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fugitive Wizard reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LGN's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FugitiveWizardReprint = Printing(
    oracleId = "381fa2a9-69fc-4558-a0ac-fb99c6f8d77f",
    name = "Fugitive Wizard",
    setCode = "8ED",
    collectorNumber = "81",
    artist = "Jim Nelson",
    imageUri = "https://cards.scryfall.io/normal/front/c/0/c0d745a5-1d5a-45ec-a662-c13cab70bfcf.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

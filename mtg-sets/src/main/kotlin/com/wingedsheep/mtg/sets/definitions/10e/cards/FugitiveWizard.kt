package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fugitive Wizard reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LGN's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FugitiveWizardReprint = Printing(
    oracleId = "381fa2a9-69fc-4558-a0ac-fb99c6f8d77f",
    name = "Fugitive Wizard",
    setCode = "10E",
    collectorNumber = "86",
    artist = "Mark Zug",
    imageUri = "https://cards.scryfall.io/normal/front/1/c/1c7be532-d353-4961-94bf-db3265515753.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

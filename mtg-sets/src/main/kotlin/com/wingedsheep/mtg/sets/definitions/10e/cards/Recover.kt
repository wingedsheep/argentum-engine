package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Recover reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * INV's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RecoverReprint = Printing(
    oracleId = "f4361602-63da-430b-adbb-36d74e987aa6",
    name = "Recover",
    setCode = "10E",
    collectorNumber = "172",
    artist = "Nelson DeCastro",
    imageUri = "https://cards.scryfall.io/normal/front/e/8/e864efca-d7c8-4ec7-8cda-ba89f9df7722.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

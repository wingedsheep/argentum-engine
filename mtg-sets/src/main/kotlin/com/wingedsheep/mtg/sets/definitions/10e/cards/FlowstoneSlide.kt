package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Flowstone Slide reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * NEM's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FlowstoneSlideReprint = Printing(
    oracleId = "ad779760-1197-4248-8e21-7a048122640f",
    name = "Flowstone Slide",
    setCode = "10E",
    collectorNumber = "203",
    artist = "Chippy",
    imageUri = "https://cards.scryfall.io/normal/front/0/7/074121e8-aecc-469f-b181-8e6a9e918826.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)

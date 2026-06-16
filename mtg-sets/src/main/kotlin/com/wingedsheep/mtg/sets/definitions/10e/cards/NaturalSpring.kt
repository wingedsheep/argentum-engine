package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Natural Spring reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val NaturalSpringReprint = Printing(
    oracleId = "f7571a2e-aaf3-4148-ab76-2a2e35273c70",
    name = "Natural Spring",
    setCode = "10E",
    collectorNumber = "281",
    artist = "Jeffrey R. Busch",
    imageUri = "https://cards.scryfall.io/normal/front/9/8/983874b1-3179-4ac6-a4f3-efa133331c6f.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

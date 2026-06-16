package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fountain of Youth reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * DRK's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FountainOfYouthReprint = Printing(
    oracleId = "b906923c-9997-4da5-a05e-53eb4d2dff32",
    name = "Fountain of Youth",
    setCode = "10E",
    collectorNumber = "323",
    artist = "Dan Murayama Scott",
    imageUri = "https://cards.scryfall.io/normal/front/8/5/8515a993-0f9d-4ac8-8452-889f23d9d9a9.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

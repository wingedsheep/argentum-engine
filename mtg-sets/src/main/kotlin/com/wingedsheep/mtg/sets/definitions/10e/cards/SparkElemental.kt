package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Spark Elemental reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * 5DN's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SparkElementalReprint = Printing(
    oracleId = "50608b7e-b6b2-40ca-91ec-db3e03dbb1af",
    name = "Spark Elemental",
    setCode = "10E",
    collectorNumber = "237",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/0/9/09e7dfad-aca9-498d-bd1c-5503b411ca5f.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

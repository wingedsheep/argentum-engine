package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Skyhunter Skirmisher reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * 5DN's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SkyhunterSkirmisherReprint = Printing(
    oracleId = "0c07d09e-e127-4573-b827-6c50246f7a31",
    name = "Skyhunter Skirmisher",
    setCode = "10E",
    collectorNumber = "43",
    artist = "Greg Staples",
    imageUri = "https://cards.scryfall.io/normal/front/c/3/c38fd8f5-6e12-4675-99bb-b493af6a9e52.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

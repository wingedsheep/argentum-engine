package com.wingedsheep.mtg.sets.definitions.`5dn`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Relic Barrier reprint in 5DN.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 5DN-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RelicBarrierReprint = Printing(
    oracleId = "90cd8274-3f21-4b78-8910-dcaa5f8fe25d",
    name = "Relic Barrier",
    setCode = "5DN",
    collectorNumber = "147",
    artist = "Nottsuo",
    imageUri = "https://cards.scryfall.io/normal/front/1/2/12d760d8-030e-47d0-a555-d88ee43d3d80.jpg",
    releaseDate = "2004-06-04",
    rarity = Rarity.UNCOMMON,
)

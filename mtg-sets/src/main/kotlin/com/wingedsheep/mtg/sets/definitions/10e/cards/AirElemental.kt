package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Air Elemental reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AirElementalReprint = Printing(
    oracleId = "7744bae4-a8b7-44a5-9b4c-0048ad4cc448",
    name = "Air Elemental",
    setCode = "10E",
    collectorNumber = "64",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/b/e/be2326dd-78c9-46bb-a459-306602939a41.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Air Elemental reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AirElementalReprint = Printing(
    oracleId = "7744bae4-a8b7-44a5-9b4c-0048ad4cc448",
    name = "Air Elemental",
    setCode = "LEB",
    collectorNumber = "47",
    artist = "Richard Thomas",
    imageUri = "https://cards.scryfall.io/normal/front/3/6/36a94a6d-26b1-4486-9444-ec366e6f4d6e.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)

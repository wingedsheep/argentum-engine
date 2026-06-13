package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * White Ward reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WhiteWardReprint = Printing(
    oracleId = "860468b3-625a-4663-a5ae-336ae10fc7d0",
    name = "White Ward",
    setCode = "LEB",
    collectorNumber = "45",
    artist = "Dan Frazier",
    imageUri = "https://cards.scryfall.io/normal/front/4/9/4988dc3e-2ed8-4de3-9d1b-838003c9c9e3.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)

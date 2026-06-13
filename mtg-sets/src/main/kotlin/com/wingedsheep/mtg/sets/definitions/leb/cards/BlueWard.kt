package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Blue Ward reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BlueWardReprint = Printing(
    oracleId = "fc0bf1d0-46a2-4305-ab2e-466d79d60ab2",
    name = "Blue Ward",
    setCode = "LEB",
    collectorNumber = "8",
    artist = "Dan Frazier",
    imageUri = "https://cards.scryfall.io/normal/front/a/a/aafae6f4-0880-4532-9224-44545bfa5eb4.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)

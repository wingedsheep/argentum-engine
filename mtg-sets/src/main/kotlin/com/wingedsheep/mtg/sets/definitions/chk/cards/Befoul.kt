package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Befoul reprint in CHK.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * USG's `cards/` package (the card's earliest real printing). This file contributes only
 * the CHK-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BefoulReprint = Printing(
    oracleId = "c573c59a-5a79-4fe1-a6d8-5e0153b5c059",
    name = "Befoul",
    setCode = "CHK",
    collectorNumber = "102",
    artist = "Luca Zontini",
    imageUri = "https://cards.scryfall.io/normal/front/2/d/2dfff5d3-1433-4a24-83e6-6361a446b974.jpg",
    releaseDate = "2004-10-01",
    rarity = Rarity.COMMON,
)

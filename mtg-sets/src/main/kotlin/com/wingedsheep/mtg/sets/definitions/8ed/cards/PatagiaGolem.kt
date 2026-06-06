package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Patagia Golem reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MIR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val PatagiaGolemReprint = Printing(
    oracleId = "85308fb8-e5a1-4ff1-8606-283cf2056895",
    name = "Patagia Golem",
    setCode = "8ED",
    collectorNumber = "308",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/c/6/c66c9c30-35c4-4813-a766-a54a418baf8b.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)

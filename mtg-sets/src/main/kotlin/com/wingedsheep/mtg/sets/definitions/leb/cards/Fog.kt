package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fog reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FogReprint = Printing(
    oracleId = "27e9db49-7af7-4bef-ad4c-bf5dfb92030d",
    name = "Fog",
    setCode = "LEB",
    collectorNumber = "194",
    artist = "Jesper Myrfors",
    imageUri = "https://cards.scryfall.io/normal/front/f/4/f4e9597a-4489-47e9-8b15-888acb402ddd.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)

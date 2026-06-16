package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Battlefield Forge reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * APC's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BattlefieldForgeReprint = Printing(
    oracleId = "6b75b94e-83b7-457e-ac41-7ca90b5a59aa",
    name = "Battlefield Forge",
    setCode = "10E",
    collectorNumber = "348",
    artist = "Darrell Riche",
    imageUri = "https://cards.scryfall.io/normal/front/e/0/e0df1e5f-8cef-4e13-babf-4254a85ac46b.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)

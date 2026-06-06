package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Royal Assassin reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RoyalAssassinReprint = Printing(
    oracleId = "9ed6f28f-a3db-48c5-9ab0-b90a7fba5f57",
    name = "Royal Assassin",
    setCode = "8ED",
    collectorNumber = "159",
    artist = "Mark Zug",
    imageUri = "https://cards.scryfall.io/normal/front/4/e/4ed8b7de-a72f-429f-a99c-438af59de19e.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)

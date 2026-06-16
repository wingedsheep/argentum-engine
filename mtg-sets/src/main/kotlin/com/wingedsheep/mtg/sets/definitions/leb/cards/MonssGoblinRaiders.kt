package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mons's Goblin Raiders reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MonssGoblinRaidersReprint = Printing(
    oracleId = "a37159df-f6d7-4db6-85de-0ea77f425993",
    name = "Mons's Goblin Raiders",
    setCode = "LEB",
    collectorNumber = "165",
    artist = "Jeff A. Menges",
    imageUri = "https://cards.scryfall.io/normal/front/2/f/2fbf039d-0ab9-4c42-a0a3-cbfa3ea1dd6e.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)

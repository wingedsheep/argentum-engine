package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Juggernaut reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val JuggernautReprint = Printing(
    oracleId = "4ac9116f-36bc-4d71-b696-d6ee064e1d58",
    name = "Juggernaut",
    setCode = "LEB",
    collectorNumber = "256",
    artist = "Dan Frazier",
    imageUri = "https://cards.scryfall.io/normal/front/8/7/870eb49c-f62d-4986-b492-601feb68a307.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)

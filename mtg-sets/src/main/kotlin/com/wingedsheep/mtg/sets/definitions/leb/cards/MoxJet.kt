package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mox Jet reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MoxJetReprint = Printing(
    oracleId = "0677f49e-f8bf-4349-af52-2ccde9287c2e",
    name = "Mox Jet",
    setCode = "LEB",
    collectorNumber = "263",
    artist = "Dan Frazier",
    imageUri = "https://cards.scryfall.io/normal/front/1/3/133204e4-fef8-4851-aa50-c96ffa35b802.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)

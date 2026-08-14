package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Tamiyo, Field Researcher reprint in BLC.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, loyalty) lives in
 * EMN's `cards/` package (the card's earliest real printing). This file contributes only
 * the BLC-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TamiyoFieldResearcherReprint = Printing(
    oracleId = "9cf99129-f51a-4418-a8cc-8ca2992b17fa",
    name = "Tamiyo, Field Researcher",
    setCode = "BLC",
    collectorNumber = "100",
    artist = "Justin Gerard",
    imageUri = "https://cards.scryfall.io/normal/front/5/8/5899d4c5-e6b8-48e9-9044-b5b34a1284f9.jpg?1783910707",
    releaseDate = "2024-08-02",
    rarity = Rarity.MYTHIC,
)

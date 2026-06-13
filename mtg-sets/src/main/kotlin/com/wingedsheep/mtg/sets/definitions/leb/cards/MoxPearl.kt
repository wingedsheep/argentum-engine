package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mox Pearl reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MoxPearlReprint = Printing(
    oracleId = "824597b8-c89a-47ec-8526-7efc6e24ef0e",
    name = "Mox Pearl",
    setCode = "LEB",
    collectorNumber = "264",
    artist = "Dan Frazier",
    imageUri = "https://cards.scryfall.io/normal/front/4/d/4da892c5-071f-416f-9e42-c4bff102eb88.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)

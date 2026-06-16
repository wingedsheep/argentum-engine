package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Rally the Peasants reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ISD's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RallyThePeasantsReprint = Printing(
    oracleId = "3f0a0a15-613a-4864-9309-a340dc4cf94d",
    name = "Rally the Peasants",
    setCode = "INR",
    collectorNumber = "37",
    artist = "Jaime Jones",
    imageUri = "https://cards.scryfall.io/normal/front/9/3/9387f821-10ec-4698-9697-ad37084b4861.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)

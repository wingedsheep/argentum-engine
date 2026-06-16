package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Ice Storm reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val IceStormReprint = Printing(
    oracleId = "a0b97e33-2d0c-4800-a9f5-9cd9be651ea8",
    name = "Ice Storm",
    setCode = "LEB",
    collectorNumber = "202",
    artist = "Dan Frazier",
    imageUri = "https://cards.scryfall.io/normal/front/7/c/7c439c5a-b4a5-411b-9e68-fb8438ccdfb0.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)

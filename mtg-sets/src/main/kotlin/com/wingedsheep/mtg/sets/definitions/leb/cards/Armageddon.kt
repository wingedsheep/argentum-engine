package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Armageddon reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ArmageddonReprint = Printing(
    oracleId = "c9ed8b01-959a-47d6-891e-0abbdccf6e4f",
    name = "Armageddon",
    setCode = "LEB",
    collectorNumber = "2",
    artist = "Jesper Myrfors",
    imageUri = "https://cards.scryfall.io/normal/front/0/2/02c4edfa-7822-40bc-88d1-d051b3a64df1.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)

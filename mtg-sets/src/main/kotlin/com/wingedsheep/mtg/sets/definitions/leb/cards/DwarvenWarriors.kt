package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dwarven Warriors reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DwarvenWarriorsReprint = Printing(
    oracleId = "cfc553cd-3b4c-47a9-bffb-e5790befb32c",
    name = "Dwarven Warriors",
    setCode = "LEB",
    collectorNumber = "144",
    artist = "Douglas Shuler",
    imageUri = "https://cards.scryfall.io/normal/front/c/0/c0de88cf-b9e5-4611-a16f-2787d8d9d269.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)

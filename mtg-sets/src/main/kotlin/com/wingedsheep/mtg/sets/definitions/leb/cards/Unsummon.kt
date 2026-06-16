package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Unsummon reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val UnsummonReprint = Printing(
    oracleId = "837182db-1bf3-4a2c-bd01-1af9d9873561",
    name = "Unsummon",
    setCode = "LEB",
    collectorNumber = "87",
    artist = "Douglas Shuler",
    imageUri = "https://cards.scryfall.io/normal/front/6/8/686843c8-8c8a-4af6-bca8-e7f7583cc886.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)

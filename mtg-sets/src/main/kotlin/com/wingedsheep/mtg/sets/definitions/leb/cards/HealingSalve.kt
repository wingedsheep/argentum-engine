package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Healing Salve reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HealingSalveReprint = Printing(
    oracleId = "8da8644c-75a1-4fe9-8e94-900d948d631c",
    name = "Healing Salve",
    setCode = "LEB",
    collectorNumber = "23",
    artist = "Dan Frazier",
    imageUri = "https://cards.scryfall.io/normal/front/9/c/9c9f2eeb-fea5-4b33-9723-8be3c1914f63.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)

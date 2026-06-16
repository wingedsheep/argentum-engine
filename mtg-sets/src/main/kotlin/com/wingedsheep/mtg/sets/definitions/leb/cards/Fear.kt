package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fear reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FearReprint = Printing(
    oracleId = "355bbe9b-59bf-470f-8600-410af4c7fe18",
    name = "Fear",
    setCode = "LEB",
    collectorNumber = "109",
    artist = "Mark Poole",
    imageUri = "https://cards.scryfall.io/normal/front/6/7/67830531-970a-4339-8673-40954376455d.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)

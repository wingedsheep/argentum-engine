package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fear reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FearReprint = Printing(
    oracleId = "355bbe9b-59bf-470f-8600-410af4c7fe18",
    name = "Fear",
    setCode = "8ED",
    collectorNumber = "134",
    artist = "Adam Rex",
    imageUri = "https://cards.scryfall.io/normal/front/b/1/b18630f0-29de-4437-9c69-5f61b6f6fda5.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

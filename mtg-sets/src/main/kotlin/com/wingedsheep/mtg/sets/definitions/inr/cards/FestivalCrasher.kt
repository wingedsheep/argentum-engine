package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Festival Crasher reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MID's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FestivalCrasherReprint = Printing(
    oracleId = "e6ddea98-b23f-44de-a63d-5d5dfad60e71",
    name = "Festival Crasher",
    setCode = "INR",
    collectorNumber = "153",
    artist = "Milivoj Ćeran",
    imageUri = "https://cards.scryfall.io/normal/front/d/8/d8d050c1-ae0c-46c1-8aa7-7cde4606105b.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)

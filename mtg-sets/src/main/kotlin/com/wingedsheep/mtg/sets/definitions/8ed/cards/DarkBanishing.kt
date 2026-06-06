package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dark Banishing reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ICE's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DarkBanishingReprint = Printing(
    oracleId = "0df450be-9bd2-45be-ae59-fa8d19f6a391",
    name = "Dark Banishing",
    setCode = "8ED",
    collectorNumber = "123",
    artist = "Dermot Power",
    imageUri = "https://cards.scryfall.io/normal/front/0/c/0c42a59e-f916-48a7-8721-091ee47a6ea1.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

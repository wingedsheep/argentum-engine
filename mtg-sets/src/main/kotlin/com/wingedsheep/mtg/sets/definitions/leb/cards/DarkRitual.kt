package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dark Ritual reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DarkRitualReprint = Printing(
    oracleId = "53f7c868-b03e-4fc2-8dcf-a75bbfa3272b",
    name = "Dark Ritual",
    setCode = "LEB",
    collectorNumber = "99",
    artist = "Sandra Everingham",
    imageUri = "https://cards.scryfall.io/normal/front/0/6/0690f724-eb95-416b-b064-f1239e2a30e8.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)

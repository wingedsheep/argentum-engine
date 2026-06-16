package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Inspiring Captain reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * SOI's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val InspiringCaptainReprint = Printing(
    oracleId = "4a294001-645b-4691-bb32-c240ba5b5320",
    name = "Inspiring Captain",
    setCode = "INR",
    collectorNumber = "28",
    artist = "Ben Maier",
    imageUri = "https://cards.scryfall.io/normal/front/f/b/fb56d6ab-50bf-4407-9f30-595ef7ae9492.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)

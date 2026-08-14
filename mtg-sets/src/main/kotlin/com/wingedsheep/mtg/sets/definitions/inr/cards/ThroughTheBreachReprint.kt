package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Through the Breach reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, splice keyword) lives in
 * CHK's `cards/` package (the card's earliest real printing). This file contributes only the
 * INR-specific presentation row — set, collector number, art — picked up automatically by
 * `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ThroughTheBreachReprint = Printing(
    oracleId = "c207aaee-0b67-4520-ad02-2c289228be2a",
    name = "Through the Breach",
    setCode = "INR",
    collectorNumber = "175",
    artist = "Randy Vargas",
    imageUri = "https://cards.scryfall.io/normal/front/c/9/c90a3430-f6d9-4432-84d3-9952d2b82003.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.MYTHIC,
)

package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Drogskol Shieldmate reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * EMN's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DrogskolShieldmateReprint = Printing(
    oracleId = "470028cf-0b99-4dd1-bb9d-aba91c52ee28",
    name = "Drogskol Shieldmate",
    setCode = "INR",
    collectorNumber = "20",
    artist = "Nils Hamm",
    imageUri = "https://cards.scryfall.io/normal/front/b/a/ba94ea90-66e5-4fd2-80fd-4baec5f8a1e4.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)

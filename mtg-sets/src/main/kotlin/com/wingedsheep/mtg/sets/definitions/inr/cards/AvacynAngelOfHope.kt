package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Avacyn, Angel of Hope reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * AVR's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AvacynAngelOfHopeReprint = Printing(
    oracleId = "216cb26e-8da9-478b-bfbc-8030f7adee72",
    name = "Avacyn, Angel of Hope",
    setCode = "INR",
    collectorNumber = "477",
    artist = "Jason Chan",
    imageUri = "https://cards.scryfall.io/normal/front/7/4/74e5dcec-ef1d-4461-bb23-61d98ff082dd.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.MYTHIC,
)

package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Soul-Guide Gryff reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MID's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SoulGuideGryffReprint = Printing(
    oracleId = "e26a08de-6108-4051-9c42-b5ce9dd8b533",
    name = "Soul-Guide Gryff",
    setCode = "INR",
    collectorNumber = "40",
    artist = "Cristi Balanescu",
    imageUri = "https://cards.scryfall.io/normal/front/3/9/39388258-cf07-4f29-a379-c743ecac680b.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)

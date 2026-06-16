package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Howlpack Resurgence reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * SOI's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HowlpackResurgenceReprint = Printing(
    oracleId = "614c3f71-e231-4c12-9e6b-cb8ea1d100bf",
    name = "Howlpack Resurgence",
    setCode = "INR",
    collectorNumber = "204",
    artist = "Izzy",
    imageUri = "https://cards.scryfall.io/normal/front/c/0/c0dc4c08-ed18-454b-b061-8901c253005c.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)

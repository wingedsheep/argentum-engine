package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Cathar Commando reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MID's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CatharCommandoReprint = Printing(
    oracleId = "774dce79-67e0-4820-8013-c7a7347993ce",
    name = "Cathar Commando",
    setCode = "INR",
    collectorNumber = "15",
    artist = "Evyn Fong",
    imageUri = "https://cards.scryfall.io/normal/front/7/c/7cd21530-ca72-4986-a0f2-142b9f23c413.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)

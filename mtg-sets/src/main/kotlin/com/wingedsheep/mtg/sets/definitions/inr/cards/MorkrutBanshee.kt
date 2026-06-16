package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Morkrut Banshee reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ISD's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MorkrutBansheeReprint = Printing(
    oracleId = "e2fe1847-4003-41c5-a64b-03afd888b81e",
    name = "Morkrut Banshee",
    setCode = "INR",
    collectorNumber = "125",
    artist = "Fajareka Setiawan",
    imageUri = "https://cards.scryfall.io/normal/front/1/4/14ab7063-1704-4c56-a58d-4f4287299bd7.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)

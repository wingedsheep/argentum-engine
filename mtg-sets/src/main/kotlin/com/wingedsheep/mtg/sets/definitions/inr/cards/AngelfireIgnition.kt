package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Angelfire Ignition reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MID's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AngelfireIgnitionReprint = Printing(
    oracleId = "29e1c6e3-d76e-4388-b4e9-f732fdec4338",
    name = "Angelfire Ignition",
    setCode = "INR",
    collectorNumber = "229",
    artist = "Yeong-Hao Han",
    imageUri = "https://cards.scryfall.io/normal/front/8/4/84ec79d8-c8de-46be-9d82-5e6bd5de9cad.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.RARE,
)

package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Heartless Summoning reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ISD's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HeartlessSummoningReprint = Printing(
    oracleId = "fa1be67e-a07e-42ce-88be-446acd643dc6",
    name = "Heartless Summoning",
    setCode = "INR",
    collectorNumber = "117",
    artist = "Anthony Palumbo",
    imageUri = "https://cards.scryfall.io/normal/front/c/5/c50fdb09-e06c-463c-87dd-3c46f4db889a.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.RARE,
)

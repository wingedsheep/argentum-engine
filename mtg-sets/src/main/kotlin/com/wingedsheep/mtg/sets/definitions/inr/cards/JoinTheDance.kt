package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Join the Dance reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MID's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val JoinTheDanceReprint = Printing(
    oracleId = "d8fc015c-7e6d-441d-bbeb-98ca74b21314",
    name = "Join the Dance",
    setCode = "INR",
    collectorNumber = "242",
    artist = "Raoul Vitale",
    imageUri = "https://cards.scryfall.io/normal/front/4/a/4a2d360c-1fad-4425-a1ab-5f713c45c7b3.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)

package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Faithless Looting reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * DKA's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FaithlessLootingReprint = Printing(
    oracleId = "3d6fa57a-aa53-4b5c-b8af-a7612c823117",
    name = "Faithless Looting",
    setCode = "INR",
    collectorNumber = "151",
    artist = "Gabor Szikszai",
    imageUri = "https://cards.scryfall.io/normal/front/5/2/52e47757-0aa9-48b0-8b43-e2483d7eed67.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)

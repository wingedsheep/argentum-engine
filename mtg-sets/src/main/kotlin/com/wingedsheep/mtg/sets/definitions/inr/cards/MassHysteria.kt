package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mass Hysteria reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MRD's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MassHysteriaReprint = Printing(
    oracleId = "4500131b-7417-4f30-a1b0-97d51b2e6458",
    name = "Mass Hysteria",
    setCode = "INR",
    collectorNumber = "164",
    artist = "Justine Cruz",
    imageUri = "https://cards.scryfall.io/normal/front/3/c/3c1854c9-11df-406d-b751-302b6f3a08fd.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.RARE,
)

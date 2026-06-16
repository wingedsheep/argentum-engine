package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Gryff's Boon reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * SOI's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GryffsBoonReprint = Printing(
    oracleId = "f62f3f4e-d506-43a9-91d8-bbc86e0a9167",
    name = "Gryff's Boon",
    setCode = "INR",
    collectorNumber = "25",
    artist = "Svetlin Velinov",
    imageUri = "https://cards.scryfall.io/normal/front/a/0/a01e7a3f-9654-4f72-8adc-20dad0b5d14e.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)

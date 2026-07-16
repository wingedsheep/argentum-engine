package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Hopeful Initiate reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in VOW's
 * `cards/` package (the card's earliest real printing). This file contributes only the
 * INR-specific presentation row — set, collector number, art — picked up automatically by
 * `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HopefulInitiateReprint = Printing(
    oracleId = "317169b8-9014-48e6-862b-ca21b706846e",
    name = "Hopeful Initiate",
    setCode = "INR",
    collectorNumber = "27",
    scryfallId = "c655f9e8-ff99-48d0-83a9-ea6a853f17c6",
    artist = "Dan Murayama Scott",
    imageUri = "https://cards.scryfall.io/normal/front/c/6/c655f9e8-ff99-48d0-83a9-ea6a853f17c6.jpg?1783908180",
    releaseDate = "2025-01-24",
    rarity = Rarity.RARE,
)

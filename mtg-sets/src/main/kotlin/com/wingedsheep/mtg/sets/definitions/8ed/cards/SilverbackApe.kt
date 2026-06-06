package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Silverback Ape reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * S99's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SilverbackApeReprint = Printing(
    oracleId = "a72f9437-d652-4a64-99df-a307a6dd9a0d",
    name = "Silverback Ape",
    setCode = "8ED",
    collectorNumber = "S7",
    artist = "Ron Spears",
    imageUri = "https://cards.scryfall.io/normal/front/0/2/025b3156-975d-4f64-b19c-172cb21266c5.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)

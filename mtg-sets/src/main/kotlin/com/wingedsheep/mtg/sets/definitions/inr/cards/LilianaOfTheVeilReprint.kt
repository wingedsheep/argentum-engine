package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Liliana of the Veil reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, loyalty) lives in
 * ISD's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LilianaOfTheVeilReprint = Printing(
    oracleId = "0ba134d8-ee7d-48ec-8dc6-57942b8e9261",
    name = "Liliana of the Veil",
    setCode = "INR",
    collectorNumber = "475",
    artist = "Steve Argyle",
    imageUri = "https://cards.scryfall.io/normal/front/e/f/efbb7256-9337-4183-8bda-a419f3f2c501.jpg?1783907977",
    releaseDate = "2025-01-24",
    rarity = Rarity.MYTHIC,
)

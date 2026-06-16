package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Demonic Taskmaster reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * AVR's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DemonicTaskmasterReprint = Printing(
    oracleId = "162e45f5-edb8-4a13-acdc-8980ce5ba8ac",
    name = "Demonic Taskmaster",
    setCode = "INR",
    collectorNumber = "104",
    artist = "Chris Rahn",
    imageUri = "https://cards.scryfall.io/normal/front/a/f/af33e187-b535-49ac-90fa-17a5ed72d920.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)

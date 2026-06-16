package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Ghost Warden reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * GPT's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GhostWardenReprint = Printing(
    oracleId = "66bd5999-742a-43e0-827c-ecb8ec7db242",
    name = "Ghost Warden",
    setCode = "10E",
    collectorNumber = "16",
    artist = "Ittoku",
    imageUri = "https://cards.scryfall.io/normal/front/2/c/2cd81534-79cb-4fde-bfa5-11c510ac6e11.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

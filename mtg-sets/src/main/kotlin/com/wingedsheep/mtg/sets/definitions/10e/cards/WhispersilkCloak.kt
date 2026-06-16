package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Whispersilk Cloak reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * DST's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WhispersilkCloakReprint = Printing(
    oracleId = "9ad4f730-a18e-4a7c-a468-a926c718c741",
    name = "Whispersilk Cloak",
    setCode = "10E",
    collectorNumber = "345",
    artist = "Luca Zontini",
    imageUri = "https://cards.scryfall.io/normal/front/6/1/6172980e-5464-4a9a-8919-9da86261d0c4.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

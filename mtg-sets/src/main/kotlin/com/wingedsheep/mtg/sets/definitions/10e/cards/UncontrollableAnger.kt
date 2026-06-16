package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Uncontrollable Anger reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * CHK's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val UncontrollableAngerReprint = Printing(
    oracleId = "6e3befe3-38c1-42d1-8825-c40eb0c6adec",
    name = "Uncontrollable Anger",
    setCode = "10E",
    collectorNumber = "244",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/1/8/18d9c85a-ba8e-40d4-ab09-1a22a7462030.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

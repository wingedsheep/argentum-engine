package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Cloud Sprite reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MMQ's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CloudSpriteReprint = Printing(
    oracleId = "283433cb-9466-4b40-8af9-30934654d43b",
    name = "Cloud Sprite",
    setCode = "10E",
    collectorNumber = "75",
    artist = "Mark Zug",
    imageUri = "https://cards.scryfall.io/normal/front/f/e/feacf831-a6e4-456b-b8f2-d7ec554281a1.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

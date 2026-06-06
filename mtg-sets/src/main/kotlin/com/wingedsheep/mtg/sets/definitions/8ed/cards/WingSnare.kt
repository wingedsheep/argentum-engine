package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wing Snare reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ULG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WingSnareReprint = Printing(
    oracleId = "61ce6ec1-79d8-4dc9-ab2d-1623ed1459c1",
    name = "Wing Snare",
    setCode = "8ED",
    collectorNumber = "288",
    artist = "Daren Bader",
    imageUri = "https://cards.scryfall.io/normal/front/e/b/eb5beaf0-21ca-4539-8773-8a9430af74b0.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)

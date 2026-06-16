package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Juggernaut reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val JuggernautReprint = Printing(
    oracleId = "4ac9116f-36bc-4d71-b696-d6ee064e1d58",
    name = "Juggernaut",
    setCode = "10E",
    collectorNumber = "328",
    artist = "Arnie Swekel",
    imageUri = "https://cards.scryfall.io/normal/front/1/9/197ac76c-34c0-4d3b-868b-aa395edb221e.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

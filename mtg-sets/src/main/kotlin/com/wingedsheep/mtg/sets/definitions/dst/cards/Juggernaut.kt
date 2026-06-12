package com.wingedsheep.mtg.sets.definitions.dst.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Juggernaut reprint in DST.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the DST-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val JuggernautReprint = Printing(
    oracleId = "4ac9116f-36bc-4d71-b696-d6ee064e1d58",
    name = "Juggernaut",
    setCode = "DST",
    collectorNumber = "125",
    artist = "Arnie Swekel",
    imageUri = "https://cards.scryfall.io/normal/front/9/d/9d159d90-2f3b-4034-bb45-41448b67fe2e.jpg",
    releaseDate = "2004-02-06",
    rarity = Rarity.UNCOMMON,
)

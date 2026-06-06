package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Seasoned Marshal reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SeasonedMarshalReprint = Printing(
    oracleId = "0c7239bf-dc8a-4d79-867e-7a4225568c49",
    name = "Seasoned Marshal",
    setCode = "8ED",
    collectorNumber = "44",
    artist = "Matthew D. Wilson",
    imageUri = "https://cards.scryfall.io/normal/front/8/0/80ae1da2-f752-481c-a0e8-3c73f6b4ccbe.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)

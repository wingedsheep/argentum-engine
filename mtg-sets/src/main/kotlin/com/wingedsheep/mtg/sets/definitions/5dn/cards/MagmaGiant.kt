package com.wingedsheep.mtg.sets.definitions.`5dn`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Magma Giant reprint in 5DN.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * P02's `cards/` package (the card's earliest real printing). This file contributes only
 * the 5DN-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MagmaGiantReprint = Printing(
    oracleId = "f71d8073-d265-4387-8d08-0b9815fd2242",
    name = "Magma Giant",
    setCode = "5DN",
    collectorNumber = "72",
    artist = "Nottsuo",
    imageUri = "https://cards.scryfall.io/normal/front/5/7/57670afe-cc40-44f5-b97c-a8f0377c710a.jpg",
    releaseDate = "2004-06-04",
    rarity = Rarity.RARE,
)

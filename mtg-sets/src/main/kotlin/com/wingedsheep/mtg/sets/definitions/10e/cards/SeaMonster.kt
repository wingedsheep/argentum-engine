package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sea Monster reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TMP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SeaMonsterReprint = Printing(
    oracleId = "21b07ce7-b4f9-438c-8c09-e624557d62d2",
    name = "Sea Monster",
    setCode = "10E",
    collectorNumber = "106",
    artist = "Brian Despain",
    imageUri = "https://cards.scryfall.io/normal/front/3/8/3809b4c6-79b4-4a06-8fc9-beabc278cbf8.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

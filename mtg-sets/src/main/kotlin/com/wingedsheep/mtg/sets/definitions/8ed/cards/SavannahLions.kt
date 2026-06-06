package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Savannah Lions reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SavannahLionsReprint = Printing(
    oracleId = "60ba93eb-39e6-4af2-9c66-cd38f72daff2",
    name = "Savannah Lions",
    setCode = "8ED",
    collectorNumber = "43",
    artist = "Carl Critchlow",
    imageUri = "https://cards.scryfall.io/normal/front/c/5/c58dbcf3-f8ad-4e82-9515-0290fa5373f7.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)

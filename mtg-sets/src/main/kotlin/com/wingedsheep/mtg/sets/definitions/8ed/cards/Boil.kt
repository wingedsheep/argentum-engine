package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Boil reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TMP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BoilReprint = Printing(
    oracleId = "3a51485b-31b2-4fa5-824a-a919f9d28ca8",
    name = "Boil",
    setCode = "8ED",
    collectorNumber = "180",
    artist = "Jason Alexander Behnke",
    imageUri = "https://cards.scryfall.io/normal/front/1/5/15a0087c-4e0f-4547-b441-b5a517c00b91.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)

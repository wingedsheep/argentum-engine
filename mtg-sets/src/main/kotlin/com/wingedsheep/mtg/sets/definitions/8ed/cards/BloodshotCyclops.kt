package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bloodshot Cyclops reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * UDS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BloodshotCyclopsReprint = Printing(
    oracleId = "84d47913-53ac-4d4d-bac3-bc0306d43e12",
    name = "Bloodshot Cyclops",
    setCode = "8ED",
    collectorNumber = "179",
    artist = "Daren Bader",
    imageUri = "https://cards.scryfall.io/normal/front/b/0/b021f1ea-4db1-40fb-a5dc-af13be53ad15.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)

package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Standing Troops reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * EXO's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val StandingTroopsReprint = Printing(
    oracleId = "9e9d5242-424d-4f61-b625-806295dbb0c7",
    name = "Standing Troops",
    setCode = "8ED",
    collectorNumber = "48",
    artist = "Daren Bader",
    imageUri = "https://cards.scryfall.io/normal/front/0/1/013a1d59-af2a-4ae1-826e-88bd77c7ce5d.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

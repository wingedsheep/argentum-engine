package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Hidden Horror reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * WTH's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HiddenHorrorReprint = Printing(
    oracleId = "db7c5a2f-9aea-4b14-86cf-7af79d6484af",
    name = "Hidden Horror",
    setCode = "10E",
    collectorNumber = "149",
    artist = "Brom",
    imageUri = "https://cards.scryfall.io/normal/front/f/4/f4ac9d57-a724-4ad1-b6a7-805381c21bb3.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

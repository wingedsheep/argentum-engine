package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Quicksand reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * VIS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val QuicksandReprint = Printing(
    oracleId = "ef2bb4fa-f292-4d19-aaa4-cfbe445caf45",
    name = "Quicksand",
    setCode = "10E",
    collectorNumber = "356",
    artist = "Roger Raupp",
    imageUri = "https://cards.scryfall.io/normal/front/8/a/8a8042ec-5306-4851-9854-f05ed7e1acf6.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

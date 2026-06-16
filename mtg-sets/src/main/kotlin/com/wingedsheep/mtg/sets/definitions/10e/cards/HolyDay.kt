package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Holy Day reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HolyDayReprint = Printing(
    oracleId = "98423a34-f044-4811-b288-56981d604b6e",
    name = "Holy Day",
    setCode = "10E",
    collectorNumber = "21",
    artist = "Volkan Baǵa",
    imageUri = "https://cards.scryfall.io/normal/front/f/3/f3895c0c-0db8-4962-b386-57887fb60701.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

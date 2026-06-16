package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Icatian Priest reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * FEM's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val IcatianPriestReprint = Printing(
    oracleId = "3a9d29ce-e8e2-402f-b0c2-873f70418234",
    name = "Icatian Priest",
    setCode = "10E",
    collectorNumber = "24",
    artist = "Stephen Tappin",
    imageUri = "https://cards.scryfall.io/normal/front/7/1/71f8c040-b6da-42f4-9140-7cd5eceb51c7.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

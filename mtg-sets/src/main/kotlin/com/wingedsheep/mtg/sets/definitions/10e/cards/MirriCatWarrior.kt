package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mirri, Cat Warrior reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * EXO's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MirriCatWarriorReprint = Printing(
    oracleId = "f01efb04-ad2b-46fb-852c-dfb7e523e1b7",
    name = "Mirri, Cat Warrior",
    setCode = "10E",
    collectorNumber = "279",
    artist = "Daren Bader",
    imageUri = "https://cards.scryfall.io/normal/front/2/c/2c4a6ac2-bec1-4d01-8d7c-e8fd43bd9e4d.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)

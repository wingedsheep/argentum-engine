package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Gray Ogre reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GrayOgreReprint = Printing(
    oracleId = "83c8a3a6-2e1a-4e26-8847-6d066f42d906",
    name = "Gray Ogre",
    setCode = "LEB",
    collectorNumber = "157",
    artist = "Dan Frazier",
    imageUri = "https://cards.scryfall.io/normal/front/4/1/41023495-d3cb-4cb0-b95c-f717480a76a5.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)

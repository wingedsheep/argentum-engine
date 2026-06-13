package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Spell Blast reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SpellBlastReprint = Printing(
    oracleId = "04477339-7ed5-4770-9e5c-6e481ffcc858",
    name = "Spell Blast",
    setCode = "LEB",
    collectorNumber = "80",
    artist = "Brian Snõddy",
    imageUri = "https://cards.scryfall.io/normal/front/3/f/3f599b73-1d55-4acc-8931-f5ab39d1d4e9.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)

package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Phantom Warrior reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val PhantomWarriorReprint = Printing(
    oracleId = "23745133-e5e2-4ce3-b94a-73d0d3d8a013",
    name = "Phantom Warrior",
    setCode = "10E",
    collectorNumber = "96",
    artist = "Greg Staples",
    imageUri = "https://cards.scryfall.io/normal/front/6/2/626ea51c-1ef0-49ed-93de-52f8f6a9d595.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

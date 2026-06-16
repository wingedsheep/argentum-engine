package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Vampire Bats reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val VampireBatsReprint = Printing(
    oracleId = "da66f1db-0fde-4112-ae8e-0e63fc686835",
    name = "Vampire Bats",
    setCode = "10E",
    collectorNumber = "186",
    artist = "Chippy",
    imageUri = "https://cards.scryfall.io/normal/front/1/1/114f37e0-0a00-4ab0-840c-8f02cea101b3.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

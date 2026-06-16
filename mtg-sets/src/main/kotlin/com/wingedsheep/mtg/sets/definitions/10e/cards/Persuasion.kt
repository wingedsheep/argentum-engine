package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Persuasion reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ODY's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val PersuasionReprint = Printing(
    oracleId = "a503d302-6b12-4d82-9ba6-73efbcb1b1a2",
    name = "Persuasion",
    setCode = "10E",
    collectorNumber = "95",
    artist = "William O'Connor",
    imageUri = "https://cards.scryfall.io/normal/front/e/9/e957fab3-7a79-4b8f-813a-daaabb0ccbea.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

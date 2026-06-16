package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Samite Healer reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SamiteHealerReprint = Printing(
    oracleId = "95a0ca48-d924-47f4-86ed-42c673ee778c",
    name = "Samite Healer",
    setCode = "10E",
    collectorNumber = "38",
    artist = "Anson Maddocks",
    imageUri = "https://cards.scryfall.io/normal/front/6/2/621f9a58-0bc8-40dd-aef1-43618274d6fe.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Trained Armodon reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TMP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TrainedArmodonReprint = Printing(
    oracleId = "4c9f5359-249e-488b-9a7e-db1a43c2b1f4",
    name = "Trained Armodon",
    setCode = "8ED",
    collectorNumber = "284",
    artist = "Gary Leach",
    imageUri = "https://cards.scryfall.io/normal/front/f/e/fe86493d-e350-40c4-b65f-de63c2f0a91a.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

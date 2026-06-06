package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Remove Soul reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RemoveSoulReprint = Printing(
    oracleId = "b13c0f76-fbda-4911-9442-c3d7e97f1aac",
    name = "Remove Soul",
    setCode = "8ED",
    collectorNumber = "95",
    artist = "Adam Rex",
    imageUri = "https://cards.scryfall.io/normal/front/b/0/b0ff7228-3040-409e-865e-999bbb779f19.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

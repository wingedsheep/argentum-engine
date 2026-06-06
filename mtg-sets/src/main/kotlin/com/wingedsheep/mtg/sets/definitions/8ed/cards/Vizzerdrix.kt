package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Vizzerdrix reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * S99's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val VizzerdrixReprint = Printing(
    oracleId = "a6649a99-c9cd-474e-91f2-cc8e75496864",
    name = "Vizzerdrix",
    setCode = "8ED",
    collectorNumber = "S5",
    artist = "Dave Dorman",
    imageUri = "https://cards.scryfall.io/normal/front/1/6/168821ce-d77c-4294-a9fd-2a30852801ab.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)

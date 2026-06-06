package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Plague Beetle reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ULG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val PlagueBeetleReprint = Printing(
    oracleId = "96415c51-2d44-4a96-9caa-9c4fbde59fca",
    name = "Plague Beetle",
    setCode = "8ED",
    collectorNumber = "154",
    artist = "Matt Cavotta",
    imageUri = "https://cards.scryfall.io/normal/front/a/1/a1476320-efed-433c-adad-cda2d2a3997f.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

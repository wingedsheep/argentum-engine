package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dusk Imp reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ODY's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DuskImpReprint = Printing(
    oracleId = "1389ae4a-c3a5-4678-9012-937a3cbaf7f7",
    name = "Dusk Imp",
    setCode = "8ED",
    collectorNumber = "130",
    artist = "Edward P. Beard, Jr.",
    imageUri = "https://cards.scryfall.io/normal/front/f/b/fba0721a-dc1f-42e4-a12a-2ecb356f9339.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

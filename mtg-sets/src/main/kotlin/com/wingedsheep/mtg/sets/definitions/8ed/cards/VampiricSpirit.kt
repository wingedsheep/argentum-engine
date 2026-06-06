package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Vampiric Spirit reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * P02's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val VampiricSpiritReprint = Printing(
    oracleId = "dbd1fc9c-6316-4c89-a971-f2e08ca291a4",
    name = "Vampiric Spirit",
    setCode = "8ED",
    collectorNumber = "170",
    artist = "Anson Maddocks",
    imageUri = "https://cards.scryfall.io/normal/front/6/e/6e5235e5-deb7-49eb-82a8-858d3ee7c91b.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)

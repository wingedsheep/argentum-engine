package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Braingeyser reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BraingeyserReprint = Printing(
    oracleId = "9908e597-9470-4c13-8387-39431b380138",
    name = "Braingeyser",
    setCode = "LEB",
    collectorNumber = "51",
    artist = "Mark Tedin",
    imageUri = "https://cards.scryfall.io/normal/front/a/5/a5dd8dbb-9538-4786-b20c-0ea2f446f323.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)

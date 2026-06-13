package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Pearled Unicorn reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val PearledUnicornReprint = Printing(
    oracleId = "c071be90-0531-40cc-af46-0cbe80c4ddd4",
    name = "Pearled Unicorn",
    setCode = "LEB",
    collectorNumber = "31",
    artist = "Cornelius Brudi",
    imageUri = "https://cards.scryfall.io/normal/front/4/7/47024d6d-dc55-4c35-b2bb-1b8bb0ee4e38.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)

package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Taiga reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TaigaReprint = Printing(
    oracleId = "22e3cf1d-3559-4ce1-954c-8dc815342979",
    name = "Taiga",
    setCode = "LEB",
    collectorNumber = "283",
    artist = "Rob Alexander",
    imageUri = "https://cards.scryfall.io/normal/front/3/0/30ce1bf0-7561-418f-a217-3ce10f28be82.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)

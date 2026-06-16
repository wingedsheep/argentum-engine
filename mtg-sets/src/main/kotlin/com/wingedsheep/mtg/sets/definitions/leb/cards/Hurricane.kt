package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Hurricane reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HurricaneReprint = Printing(
    oracleId = "9c021685-4017-49c7-9f58-2ae0243361a0",
    name = "Hurricane",
    setCode = "LEB",
    collectorNumber = "201",
    artist = "Dameon Willich",
    imageUri = "https://cards.scryfall.io/normal/front/b/3/b3939f72-1ec6-4b2c-b37e-b1ebb024bb8f.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)

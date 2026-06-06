package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Reflexes reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * USG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ReflexesReprint = Printing(
    oracleId = "dc87b0a5-3d9d-44eb-b415-b022acd63cf1",
    name = "Reflexes",
    setCode = "8ED",
    collectorNumber = "213",
    artist = "Steve White",
    imageUri = "https://cards.scryfall.io/normal/front/8/a/8a406184-b678-4afd-9599-d1d4f9c1d147.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

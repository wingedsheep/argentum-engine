package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fire Elemental reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FireElementalReprint = Printing(
    oracleId = "3912d21e-1ebc-4a81-9dc9-f404248d564a",
    name = "Fire Elemental",
    setCode = "LEB",
    collectorNumber = "149",
    artist = "Melissa A. Benson",
    imageUri = "https://cards.scryfall.io/normal/front/3/7/376cb9e5-89fb-4091-8a20-140bb6de0ef6.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)

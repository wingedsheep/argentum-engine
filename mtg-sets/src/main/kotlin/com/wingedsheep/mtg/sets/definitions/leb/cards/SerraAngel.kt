package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Serra Angel reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SerraAngelReprint = Printing(
    oracleId = "4b7ac066-e5c7-43e6-9e7e-2739b24a905d",
    name = "Serra Angel",
    setCode = "LEB",
    collectorNumber = "40",
    artist = "Douglas Shuler",
    imageUri = "https://cards.scryfall.io/normal/front/5/6/5669f9c8-2e94-47e2-a551-7efff317fb34.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)

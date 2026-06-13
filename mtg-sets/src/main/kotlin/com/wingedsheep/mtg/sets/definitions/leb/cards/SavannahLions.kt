package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Savannah Lions reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SavannahLionsReprint = Printing(
    oracleId = "60ba93eb-39e6-4af2-9c66-cd38f72daff2",
    name = "Savannah Lions",
    setCode = "LEB",
    collectorNumber = "39",
    artist = "Daniel Gelon",
    imageUri = "https://cards.scryfall.io/normal/front/6/7/67d1945d-d228-4dc3-a593-859408b2016b.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)

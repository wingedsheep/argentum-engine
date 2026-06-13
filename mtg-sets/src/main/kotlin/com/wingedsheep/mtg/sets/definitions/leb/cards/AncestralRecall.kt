package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Ancestral Recall reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AncestralRecallReprint = Printing(
    oracleId = "550c74d4-1fcb-406a-b02a-639a760a4380",
    name = "Ancestral Recall",
    setCode = "LEB",
    collectorNumber = "48",
    artist = "Mark Poole",
    imageUri = "https://cards.scryfall.io/normal/front/4/6/46b0a5c2-ac85-448e-9e87-12fc74fd4147.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)

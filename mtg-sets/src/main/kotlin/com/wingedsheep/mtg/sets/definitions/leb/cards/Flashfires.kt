package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Flashfires reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FlashfiresReprint = Printing(
    oracleId = "c281f436-8c77-48f7-b31c-d40cd7f9ed6a",
    name = "Flashfires",
    setCode = "LEB",
    collectorNumber = "152",
    artist = "Dameon Willich",
    imageUri = "https://cards.scryfall.io/normal/front/5/a/5a2a91b9-c45f-4e3d-b3c4-944493bdd86a.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)

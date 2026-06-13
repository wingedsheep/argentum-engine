package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sol Ring reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SolRingReprint = Printing(
    oracleId = "6ad8011d-3471-4369-9d68-b264cc027487",
    name = "Sol Ring",
    setCode = "LEB",
    collectorNumber = "270",
    artist = "Mark Tedin",
    imageUri = "https://cards.scryfall.io/normal/front/c/0/c0fb91ec-20a8-4c13-9469-18885b1ecca3.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)

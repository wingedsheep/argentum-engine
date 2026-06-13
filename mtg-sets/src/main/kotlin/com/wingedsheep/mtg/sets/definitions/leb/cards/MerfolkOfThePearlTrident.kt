package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Merfolk of the Pearl Trident reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MerfolkOfThePearlTridentReprint = Printing(
    oracleId = "218d9277-c179-4de3-9c7f-79b5a6d4fa38",
    name = "Merfolk of the Pearl Trident",
    setCode = "LEB",
    collectorNumber = "67",
    artist = "Jeff A. Menges",
    imageUri = "https://cards.scryfall.io/normal/front/c/c/cca142de-906d-4143-8f77-4acea1f1e6b1.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)

package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Timber Wolves reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TimberWolvesReprint = Printing(
    oracleId = "35d07ac9-b184-4b5f-8192-34b1db042f69",
    name = "Timber Wolves",
    setCode = "LEB",
    collectorNumber = "220",
    artist = "Melissa A. Benson",
    imageUri = "https://cards.scryfall.io/normal/front/a/a/aa598db8-c0c7-4a9a-bd89-6d3da0d3dfba.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)

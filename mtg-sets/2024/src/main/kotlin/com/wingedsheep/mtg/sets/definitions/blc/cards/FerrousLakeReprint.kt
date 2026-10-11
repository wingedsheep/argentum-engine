package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Ferrous Lake reprint in BLC.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * another set's `cards/` package. This file contributes only the BLC-specific
 * presentation row — set, collector number, art — picked up automatically by
 * `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FerrousLakeReprint = Printing(
    oracleId = "62c15af0-40e1-407d-b056-7a3d909e3fdb",
    name = "Ferrous Lake",
    setCode = "BLC",
    collectorNumber = "303",
    scryfallId = "eb7409f7-8fbd-49f4-8226-9013fef17ab3",
    artist = "Grady Frederick",
    imageUri = "https://cards.scryfall.io/normal/front/e/b/eb7409f7-8fbd-49f4-8226-9013fef17ab3.jpg?1783910640",
    releaseDate = "2024-08-02",
    rarity = Rarity.RARE,
)

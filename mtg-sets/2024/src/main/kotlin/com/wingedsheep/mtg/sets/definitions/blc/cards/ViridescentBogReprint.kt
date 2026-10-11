package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Viridescent Bog reprint in BLC.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * another set's `cards/` package. This file contributes only the BLC-specific
 * presentation row — set, collector number, art — picked up automatically by
 * `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ViridescentBogReprint = Printing(
    oracleId = "6bd6d259-1af7-4dff-a79c-48a616d2a36e",
    name = "Viridescent Bog",
    setCode = "BLC",
    collectorNumber = "352",
    scryfallId = "991fca57-eb30-40e7-a443-784a104625b5",
    artist = "Piotr Dura",
    imageUri = "https://cards.scryfall.io/normal/front/9/9/991fca57-eb30-40e7-a443-784a104625b5.jpg?1783910623",
    releaseDate = "2024-08-02",
    rarity = Rarity.RARE,
)

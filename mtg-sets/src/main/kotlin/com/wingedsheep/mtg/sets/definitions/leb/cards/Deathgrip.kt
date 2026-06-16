package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Deathgrip reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DeathgripReprint = Printing(
    oracleId = "20ae75a7-14ca-4366-af0a-3f3f02159f3f",
    name = "Deathgrip",
    setCode = "LEB",
    collectorNumber = "101",
    artist = "Anson Maddocks",
    imageUri = "https://cards.scryfall.io/normal/front/c/9/c942a9af-e449-4f10-916c-6eb9e944de6a.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)

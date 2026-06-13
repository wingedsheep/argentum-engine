package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Celestial Prism reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CelestialPrismReprint = Printing(
    oracleId = "eb228a6c-bceb-45c3-a258-3f209682e1c6",
    name = "Celestial Prism",
    setCode = "LEB",
    collectorNumber = "235",
    artist = "Amy Weber",
    imageUri = "https://cards.scryfall.io/normal/front/2/4/243c5460-8d4c-47a7-8a9c-ab626daa520a.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)

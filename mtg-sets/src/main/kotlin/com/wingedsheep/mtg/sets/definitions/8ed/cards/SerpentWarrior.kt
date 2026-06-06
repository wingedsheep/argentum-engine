package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Serpent Warrior reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SerpentWarriorReprint = Printing(
    oracleId = "fe249f27-db6c-4826-836e-a04efb1a3eaa",
    name = "Serpent Warrior",
    setCode = "8ED",
    collectorNumber = "161",
    artist = "Ron Spencer",
    imageUri = "https://cards.scryfall.io/normal/front/d/f/dfcf5fb1-391c-472a-8b06-503162e458d7.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

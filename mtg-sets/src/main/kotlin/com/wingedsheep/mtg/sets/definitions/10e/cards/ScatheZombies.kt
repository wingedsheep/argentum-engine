package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Scathe Zombies reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ScatheZombiesReprint = Printing(
    oracleId = "e0fefaf0-da20-4d58-8db7-019dba16c780",
    name = "Scathe Zombies",
    setCode = "10E",
    collectorNumber = "175",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/0/3/037cbf71-1199-4457-9b09-f66e7cb294d5.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

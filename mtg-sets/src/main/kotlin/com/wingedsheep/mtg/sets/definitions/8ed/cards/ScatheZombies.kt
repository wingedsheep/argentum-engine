package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Scathe Zombies reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ScatheZombiesReprint = Printing(
    oracleId = "e0fefaf0-da20-4d58-8db7-019dba16c780",
    name = "Scathe Zombies",
    setCode = "8ED",
    collectorNumber = "160",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/0/3/03fd1dae-ccad-4f05-b720-b4b8600a298b.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)

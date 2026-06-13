package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Scathe Zombies reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ScatheZombiesReprint = Printing(
    oracleId = "e0fefaf0-da20-4d58-8db7-019dba16c780",
    name = "Scathe Zombies",
    setCode = "LEB",
    collectorNumber = "126",
    artist = "Jesper Myrfors",
    imageUri = "https://cards.scryfall.io/normal/front/a/3/a30abb09-2f80-46cf-a839-b4dac5c23dce.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)

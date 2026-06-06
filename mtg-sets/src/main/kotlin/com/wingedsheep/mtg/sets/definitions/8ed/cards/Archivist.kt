package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Archivist reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ULG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ArchivistReprint = Printing(
    oracleId = "d137586f-83b0-40af-8100-443460b07ac0",
    name = "Archivist",
    setCode = "8ED",
    collectorNumber = "60",
    artist = "Donato Giancola",
    imageUri = "https://cards.scryfall.io/normal/front/0/8/083507d3-b28f-4e84-909b-bca2a2131233.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)

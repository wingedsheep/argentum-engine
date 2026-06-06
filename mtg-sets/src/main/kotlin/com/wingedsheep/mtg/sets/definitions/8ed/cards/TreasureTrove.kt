package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Treasure Trove reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * EXO's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TreasureTroveReprint = Printing(
    oracleId = "3c4a6b34-a9bb-426d-908c-7ae0011591df",
    name = "Treasure Trove",
    setCode = "8ED",
    collectorNumber = "110",
    artist = "Brian Despain",
    imageUri = "https://cards.scryfall.io/normal/front/b/4/b4f65e7a-be66-4a57-9e33-c1b02285f65e.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)

package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Tunnel reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TunnelReprint = Printing(
    oracleId = "80559618-9dd9-4987-b3bc-1a1b5537bbc5",
    name = "Tunnel",
    setCode = "LEB",
    collectorNumber = "179",
    artist = "Dan Frazier",
    imageUri = "https://cards.scryfall.io/normal/front/c/c/cc738025-a771-4186-b08c-7b37c0e9713b.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)

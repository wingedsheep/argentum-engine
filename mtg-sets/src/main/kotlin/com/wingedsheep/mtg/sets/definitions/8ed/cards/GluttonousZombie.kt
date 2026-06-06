package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Gluttonous Zombie reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ONS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GluttonousZombieReprint = Printing(
    oracleId = "6d329bd2-8825-4de9-b3de-749625085015",
    name = "Gluttonous Zombie",
    setCode = "8ED",
    collectorNumber = "136",
    artist = "Thomas M. Baxa",
    imageUri = "https://cards.scryfall.io/normal/front/4/7/4730915e-db5a-4a66-b570-aeaaa7c4ca3d.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)

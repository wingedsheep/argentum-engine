package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Staunch Defenders reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TMP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val StaunchDefendersReprint = Printing(
    oracleId = "ec8bf245-4b2c-432b-9b2a-8d7b9224258c",
    name = "Staunch Defenders",
    setCode = "8ED",
    collectorNumber = "49",
    artist = "Tristan Elwell",
    imageUri = "https://cards.scryfall.io/normal/front/0/4/04a162ae-dfe1-43e0-adf7-3d1365f3a681.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)

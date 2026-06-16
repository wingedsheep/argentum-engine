package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Demonic Tutor reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DemonicTutorReprint = Printing(
    oracleId = "82004860-e589-4e38-8d61-8c0210e4ea39",
    name = "Demonic Tutor",
    setCode = "LEB",
    collectorNumber = "105",
    artist = "Douglas Shuler",
    imageUri = "https://cards.scryfall.io/normal/front/a/5/a5e571ef-1645-4584-ab53-e7ea5d443dea.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)

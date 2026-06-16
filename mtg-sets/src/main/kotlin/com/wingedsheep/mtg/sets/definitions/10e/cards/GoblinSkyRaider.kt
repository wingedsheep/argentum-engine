package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Goblin Sky Raider reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ONS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GoblinSkyRaiderReprint = Printing(
    oracleId = "9e0ebf3b-9295-43a9-9b5c-4ffac6a6b630",
    name = "Goblin Sky Raider",
    setCode = "10E",
    collectorNumber = "210",
    artist = "Daren Bader",
    imageUri = "https://cards.scryfall.io/normal/front/e/b/ebbd596b-56c2-475c-93e4-c72f9f29281b.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)

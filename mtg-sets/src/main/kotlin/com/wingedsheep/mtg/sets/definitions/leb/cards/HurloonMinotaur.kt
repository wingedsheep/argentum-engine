package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Hurloon Minotaur reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HurloonMinotaurReprint = Printing(
    oracleId = "8f1dae40-b307-446e-bbd2-86aa35813871",
    name = "Hurloon Minotaur",
    setCode = "LEB",
    collectorNumber = "159",
    artist = "Anson Maddocks",
    imageUri = "https://cards.scryfall.io/normal/front/8/e/8ef29573-99a1-42fc-8941-2466cda2465f.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)

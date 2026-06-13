package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Obsianus Golem reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ObsianusGolemReprint = Printing(
    oracleId = "ac41171e-c454-49e9-9004-c082ae099630",
    name = "Obsianus Golem",
    setCode = "LEB",
    collectorNumber = "268",
    artist = "Jesper Myrfors",
    imageUri = "https://cards.scryfall.io/normal/front/e/9/e9ed6669-e340-46d5-906b-e24e76464e75.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)

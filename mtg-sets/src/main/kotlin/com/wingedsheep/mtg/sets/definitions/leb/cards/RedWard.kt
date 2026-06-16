package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Red Ward reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RedWardReprint = Printing(
    oracleId = "73f84440-425f-4cf5-b01a-3ae89f1f6e37",
    name = "Red Ward",
    setCode = "LEB",
    collectorNumber = "34",
    artist = "Dan Frazier",
    imageUri = "https://cards.scryfall.io/normal/front/0/5/057237bb-e1e6-4bcc-8639-ca0dcdd4846c.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)

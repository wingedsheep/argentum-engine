package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Arcanis the Omnipotent reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ONS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ArcanisTheOmnipotentReprint = Printing(
    oracleId = "8a7183cc-161c-444d-a889-a17519c8061b",
    name = "Arcanis the Omnipotent",
    setCode = "10E",
    collectorNumber = "66",
    artist = "Justin Sweet",
    imageUri = "https://cards.scryfall.io/normal/front/0/e/0ed53483-4bec-491b-9ae6-afc6faf122a2.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)

package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Ornithopter reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ATQ's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val OrnithopterReprint = Printing(
    oracleId = "a3a98bc9-caa0-49b7-951c-fe4e4f54e4ba",
    name = "Ornithopter",
    setCode = "10E",
    collectorNumber = "336",
    artist = "Dana Knutson",
    imageUri = "https://cards.scryfall.io/normal/front/8/d/8d784214-dc69-42d4-b897-995ca5751e13.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

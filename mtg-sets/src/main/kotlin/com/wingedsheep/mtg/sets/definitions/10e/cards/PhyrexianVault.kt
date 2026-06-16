package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Phyrexian Vault reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MIR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val PhyrexianVaultReprint = Printing(
    oracleId = "b628150b-08a1-4ea3-978d-60255dfb0b7e",
    name = "Phyrexian Vault",
    setCode = "10E",
    collectorNumber = "337",
    artist = "Hannibal King",
    imageUri = "https://cards.scryfall.io/normal/front/d/a/da70e601-ec60-434e-af9e-eb0bf2a580e2.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

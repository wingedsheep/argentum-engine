package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Geistcatcher's Rig reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ISD's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GeistcatchersRigReprint = Printing(
    oracleId = "1bfdbb96-cc18-4609-ba17-d169967df8d0",
    name = "Geistcatcher's Rig",
    setCode = "INR",
    collectorNumber = "264",
    artist = "Vincent Proce",
    imageUri = "https://cards.scryfall.io/normal/front/0/b/0b8f741c-919e-457d-8a02-c7282c1305ec.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)

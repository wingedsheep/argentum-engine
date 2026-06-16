package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Slayer of the Wicked reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ISD's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SlayerOfTheWickedReprint = Printing(
    oracleId = "d93299d4-3aaf-48fb-8227-26203963c8b9",
    name = "Slayer of the Wicked",
    setCode = "INR",
    collectorNumber = "39",
    artist = "Anthony Palumbo",
    imageUri = "https://cards.scryfall.io/normal/front/c/a/ca4537d3-c481-49ac-826c-b51c2e9b1fcf.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)

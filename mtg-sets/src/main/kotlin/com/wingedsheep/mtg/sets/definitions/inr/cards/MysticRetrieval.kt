package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mystic Retrieval reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * DKA's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MysticRetrievalReprint = Printing(
    oracleId = "b7cfbfa1-dd38-47ff-9138-fa6b247afc11",
    name = "Mystic Retrieval",
    setCode = "INR",
    collectorNumber = "77",
    artist = "Scott Chou",
    imageUri = "https://cards.scryfall.io/normal/front/8/0/806e5536-103b-4d6c-83b4-8659a55b25b5.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)

package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Ancient Grudge reprint in ISD.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TSP's `cards/` package (the card's earliest real printing). This file contributes only
 * the ISD-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AncientGrudgeReprint = Printing(
    oracleId = "306593b0-6ea8-476f-a3f2-e17876c1bab4",
    name = "Ancient Grudge",
    setCode = "ISD",
    collectorNumber = "127",
    artist = "Ryan Yee",
    imageUri = "https://cards.scryfall.io/normal/front/e/5/e5e7b966-7c5b-44e6-a6df-4bd7af4edaa9.jpg",
    releaseDate = "2011-09-30",
    rarity = Rarity.COMMON,
)

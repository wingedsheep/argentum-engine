package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Karplusan Forest reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ICE's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val KarplusanForestReprint = Printing(
    oracleId = "bd912666-f37f-4767-af6f-9e6d0fcccacf",
    name = "Karplusan Forest",
    setCode = "10E",
    collectorNumber = "354",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/d/5/d56ad1cb-c1b9-4856-8d46-55012aee4d47.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)

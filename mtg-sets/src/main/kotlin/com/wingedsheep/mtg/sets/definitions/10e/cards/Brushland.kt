package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Brushland reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ICE's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BrushlandReprint = Printing(
    oracleId = "5eb8b497-ec9a-4a89-ad29-1ec3ca82da7c",
    name = "Brushland",
    setCode = "10E",
    collectorNumber = "349",
    artist = "Scott Bailey",
    imageUri = "https://cards.scryfall.io/normal/front/7/0/70afcfca-c065-4a33-95b1-ec2b08bcb493.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)

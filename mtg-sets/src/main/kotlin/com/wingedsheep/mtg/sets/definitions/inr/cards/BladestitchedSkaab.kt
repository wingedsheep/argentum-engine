package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bladestitched Skaab reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MID's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BladestitchedSkaabReprint = Printing(
    oracleId = "d72b8254-3df8-431b-955a-ec2aea493e2b",
    name = "Bladestitched Skaab",
    setCode = "INR",
    collectorNumber = "231",
    artist = "Dave Kendall",
    imageUri = "https://cards.scryfall.io/normal/front/a/f/af14d311-0fd5-4979-bc46-1421472c3b0a.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)

package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fireshrieker reprint in FDN.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MRD's `cards/` package (the card's earliest real printing). This file contributes only
 * the FDN-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FireshriekerReprint = Printing(
    oracleId = "a02e1ca7-23c5-41e3-a744-72fc9e9dd8ba",
    name = "Fireshrieker",
    setCode = "FDN",
    collectorNumber = "674",
    artist = "Christopher Moeller",
    imageUri = "https://cards.scryfall.io/normal/front/4/8/484fbce6-71bd-40eb-a71b-86958a094708.jpg",
    releaseDate = "2024-11-15",
    rarity = Rarity.UNCOMMON,
)

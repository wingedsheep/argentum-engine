package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Spitting Spider reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * PCY's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SpittingSpiderReprint = Printing(
    oracleId = "24ddcf3c-a4f3-4373-9483-5ce6e50d6150",
    name = "Spitting Spider",
    setCode = "8ED",
    collectorNumber = "280",
    artist = "Edward P. Beard, Jr.",
    imageUri = "https://cards.scryfall.io/normal/front/b/f/bfeeb515-7d3d-4678-a480-32485b197afb.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)

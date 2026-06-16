package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Viridian Shaman reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MRD's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ViridianShamanReprint = Printing(
    oracleId = "5aa6d553-a144-4c5b-83fa-65e931903899",
    name = "Viridian Shaman",
    setCode = "10E",
    collectorNumber = "308",
    artist = "Scott M. Fischer",
    imageUri = "https://cards.scryfall.io/normal/front/c/8/c82082bf-5b15-4835-93ad-2263f0b0a62c.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

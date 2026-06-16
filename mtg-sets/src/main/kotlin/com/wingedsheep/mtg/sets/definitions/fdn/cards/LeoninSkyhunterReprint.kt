package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Leonin Skyhunter reprint in FDN.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MRD's `cards/` package (the card's earliest real printing). This file contributes only
 * the FDN-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LeoninSkyhunterReprint = Printing(
    oracleId = "517c7295-8ba2-47a6-a1c6-aee722f8ca88",
    name = "Leonin Skyhunter",
    setCode = "FDN",
    collectorNumber = "498",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/2/1/218e0009-5f11-4348-97b8-4bc7b41f80b8.jpg",
    releaseDate = "2024-11-15",
    rarity = Rarity.UNCOMMON,
)

package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Valorous Stance reprint in FDN.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * FRF's `cards/` package (the card's earliest real printing). This file contributes only
 * the FDN-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ValorousStanceReprint = Printing(
    oracleId = "1f938353-9081-4dc1-b8e7-d18ece038bd4",
    name = "Valorous Stance",
    setCode = "FDN",
    collectorNumber = "583",
    artist = "Willian Murai",
    imageUri = "https://cards.scryfall.io/normal/front/b/c/bc4c47e3-04b6-4760-9a80-b6acef1e4524.jpg",
    releaseDate = "2024-11-15",
    rarity = Rarity.UNCOMMON,
)

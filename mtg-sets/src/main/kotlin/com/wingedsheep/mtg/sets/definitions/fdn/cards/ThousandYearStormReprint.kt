package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Thousand-Year Storm reprint in FDN.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, rulings) lives in the
 * card's earliest real printing, Guilds of Ravnica. This file contributes only the FDN-specific
 * presentation row — set, collector number, art, rarity (mythic in GRN, rare here) — picked up
 * automatically by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ThousandYearStormReprint = Printing(
    oracleId = "dd4cf149-2fae-40e5-b50b-639f6bcec65e",
    name = "Thousand-Year Storm",
    setCode = "FDN",
    collectorNumber = "248",
    scryfallId = "76c48a67-1410-40f1-9b93-0172d85e4688",
    artist = "Dimitar Marinski",
    imageUri = "https://cards.scryfall.io/normal/front/7/6/76c48a67-1410-40f1-9b93-0172d85e4688.jpg?1783909050",
    releaseDate = "2024-11-15",
    rarity = Rarity.RARE,
)

package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Battleground Geist reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ISD's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BattlegroundGeistReprint = Printing(
    oracleId = "2450d35b-7d87-4885-b396-f8e1d3ac0b22",
    name = "Battleground Geist",
    setCode = "INR",
    collectorNumber = "53",
    artist = "Clint Cearley",
    imageUri = "https://cards.scryfall.io/normal/front/3/5/3507e285-fabb-45e3-af4c-52032558cf03.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)

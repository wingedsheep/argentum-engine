package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Undying Malice reprint in FDN.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (spell script) lives in VOW's
 * `cards/` package (the card's earliest real printing). This file contributes only the
 * FDN-specific presentation row — set, collector number, art — picked up automatically by
 * `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val UndyingMaliceReprint = Printing(
    oracleId = "8b061702-1bc3-45aa-a758-446b25034801",
    name = "Undying Malice",
    setCode = "FDN",
    collectorNumber = "528",
    scryfallId = "97b3cf11-e352-4ee1-8c03-13898f576ef9",
    artist = "Igor Kieryluk",
    imageUri = "https://cards.scryfall.io/normal/front/9/7/97b3cf11-e352-4ee1-8c03-13898f576ef9.jpg?1783908956",
    releaseDate = "2024-11-15",
    rarity = Rarity.COMMON,
)

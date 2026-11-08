package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Shiny Impetus reprint in TDC. Canonical CardDefinition lives in its earliest set.
 */
val ShinyImpetusReprint = Printing(
    oracleId = "aae76e5c-f5e0-4d18-b465-e6a829be908a",
    name = "Shiny Impetus",
    setCode = "TDC",
    collectorNumber = "231",
    scryfallId = "cc36705f-9b1a-42fe-bebd-f23c7af8a368",
    artist = "Filipe Pagliuso",
    imageUri = "https://cards.scryfall.io/normal/front/c/c/cc36705f-9b1a-42fe-bebd-f23c7af8a368.jpg?1783907061",
    releaseDate = "2025-04-11",
    rarity = Rarity.COMMON,
    frameEffects = listOf("enchantment"),
)

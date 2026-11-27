package com.wingedsheep.mtg.sets.definitions.sld.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Alchemist's Refuge reprint in SLD. Canonical CardDefinition lives in its earliest set (AVR).
 */
val AlchemistsRefugeReprint = Printing(
    oracleId = "357ed28b-899f-404b-94ff-6fb2ef81d87b",
    name = "Alchemist's Refuge",
    setCode = "SLD",
    collectorNumber = "358",
    scryfallId = "4a6cf714-29b9-4c06-9412-d958a5ffbd66",
    artist = "DZO",
    imageUri = "https://cards.scryfall.io/normal/front/4/a/4a6cf714-29b9-4c06-9412-d958a5ffbd66.jpg?1783925164",
    releaseDate = "2021-10-15",
    rarity = Rarity.RARE,
    frameEffects = listOf("inverted"),
)

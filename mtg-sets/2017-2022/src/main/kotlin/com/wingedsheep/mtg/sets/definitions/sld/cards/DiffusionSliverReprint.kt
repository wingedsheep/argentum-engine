package com.wingedsheep.mtg.sets.definitions.sld.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Diffusion Sliver reprint in SLD. Canonical CardDefinition lives in its earliest set (M15).
 */
val DiffusionSliverReprint = Printing(
    oracleId = "458ca9f4-4622-48e1-9ca1-6a8117188973",
    name = "Diffusion Sliver",
    setCode = "SLD",
    collectorNumber = "618",
    scryfallId = "810076dc-e8e8-4fa4-aa2c-07ab4f9a9a11",
    artist = "Trevor Claxton",
    imageUri = "https://cards.scryfall.io/normal/front/8/1/810076dc-e8e8-4fa4-aa2c-07ab4f9a9a11.jpg?1783921561",
    releaseDate = "2022-08-13",
    rarity = Rarity.RARE,
    frameEffects = listOf("extendedart"),
)

package com.wingedsheep.mtg.sets.definitions.sld.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sol Ring reprint in SLD. Canonical CardDefinition lives in its earliest set.
 */
val SolRingReprint = Printing(
    oracleId = "6ad8011d-3471-4369-9d68-b264cc027487",
    name = "Sol Ring",
    setCode = "SLD",
    collectorNumber = "249",
    scryfallId = "7bf22be9-bf1a-48ec-a825-d19504f9dfcc",
    artist = "Thomas Zenteno",
    imageUri = "https://cards.scryfall.io/normal/front/7/b/7bf22be9-bf1a-48ec-a825-d19504f9dfcc.jpg",
    releaseDate = "2021-02-22",
    rarity = Rarity.RARE,
)

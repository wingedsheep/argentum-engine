package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Revitalize reprint in M21.
 * Canonical CardDefinition lives in Core Set 2019 (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.m19.cards.Revitalize`.
 */
val RevitalizeM21Reprint = Printing(
    oracleId = "b1385b03-cb4b-4812-857f-7421f1df39af",
    name = "Revitalize",
    setCode = "M21",
    collectorNumber = "31",
    scryfallId = "a3bd4bce-8ab0-40b9-aad7-7d57a011bb0b",
    artist = "Matt Stewart",
    imageUri = "https://cards.scryfall.io/normal/front/a/3/a3bd4bce-8ab0-40b9-aad7-7d57a011bb0b.jpg?1783930737",
    releaseDate = "2020-07-03",
    rarity = Rarity.COMMON,
)

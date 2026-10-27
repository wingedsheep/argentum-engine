package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Revitalize reprint in KHM.
 * Canonical CardDefinition lives in Core Set 2019 (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.m19.cards.Revitalize`.
 */
val RevitalizeKhmReprint = Printing(
    oracleId = "b1385b03-cb4b-4812-857f-7421f1df39af",
    name = "Revitalize",
    setCode = "KHM",
    collectorNumber = "23",
    scryfallId = "3a9fb75e-c8e5-417b-83d4-5105af9c66c1",
    artist = "Mathias Kollros",
    imageUri = "https://cards.scryfall.io/normal/front/3/a/3a9fb75e-c8e5-417b-83d4-5105af9c66c1.jpg?1783928280",
    releaseDate = "2021-02-05",
    rarity = Rarity.COMMON,
)

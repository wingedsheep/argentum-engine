package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Woodland Champion reprint in J22. Canonical CardDefinition lives in Core Set 2020 (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.m20.cards.WoodlandChampion`.
 */
val WoodlandChampionReprint = Printing(
    oracleId = "675321d7-2cf0-4e0b-9517-d711b22865ab",
    name = "Woodland Champion",
    setCode = "J22",
    collectorNumber = "745",
    scryfallId = "03c0f16f-55c5-4334-bcec-08bf46d71d88",
    artist = "Randy Vargas",
    imageUri = "https://cards.scryfall.io/normal/front/0/3/03c0f16f-55c5-4334-bcec-08bf46d71d88.jpg?1783918824",
    releaseDate = "2022-12-02",
    rarity = Rarity.UNCOMMON,
)

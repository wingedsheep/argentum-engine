package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Arrest reprint in RTR. Canonical CardDefinition lives in Mercadian Masques (its earliest
 * real printing), `com.wingedsheep.mtg.sets.definitions.mmq.cards.Arrest`.
 */
val ArrestReprint = Printing(
    oracleId = "81728b98-8cf9-4734-a318-69184bb4d15c",
    name = "Arrest",
    setCode = "RTR",
    collectorNumber = "3",
    scryfallId = "498f74a2-7e5e-4082-97e7-b938d703f869",
    artist = "Greg Staples",
    imageUri = "https://cards.scryfall.io/normal/front/4/9/498f74a2-7e5e-4082-97e7-b938d703f869.jpg?1783940379",
    releaseDate = "2012-10-05",
    rarity = Rarity.UNCOMMON,
)

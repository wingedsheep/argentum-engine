package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lure reprint in 8ED, in both the ordinary and the foil-variant collector number.
 * Canonical CardDefinition lives in Limited Edition Alpha (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.lea.cards.Lure`.
 */
val Lure8edReprint = Printing(
    oracleId = "7a7425ba-4478-4bc4-855f-abf947ea4fa2",
    name = "Lure",
    setCode = "8ED",
    collectorNumber = "263",
    scryfallId = "a5362cf9-a55b-4df5-941e-d738a68281cf",
    artist = "DiTerlizzi",
    imageUri = "https://cards.scryfall.io/normal/front/a/5/a5362cf9-a55b-4df5-941e-d738a68281cf.jpg?1783944667",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)

val Lure8edReprintB = Printing(
    oracleId = "7a7425ba-4478-4bc4-855f-abf947ea4fa2",
    name = "Lure",
    setCode = "8ED",
    collectorNumber = "263★",
    scryfallId = "e502853e-5451-44c8-a42a-440f2387d604",
    artist = "DiTerlizzi",
    imageUri = "https://cards.scryfall.io/normal/front/e/5/e502853e-5451-44c8-a42a-440f2387d604.jpg?1783944671",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)

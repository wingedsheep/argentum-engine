package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lure reprint in 10E, in both the ordinary and the foil-variant collector number.
 * Canonical CardDefinition lives in Limited Edition Alpha (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.lea.cards.Lure`.
 */
val Lure10eReprint = Printing(
    oracleId = "7a7425ba-4478-4bc4-855f-abf947ea4fa2",
    name = "Lure",
    setCode = "10E",
    collectorNumber = "276",
    scryfallId = "d7e36efa-266f-48f1-b352-862920487f58",
    artist = "D. Alexander Gregory",
    imageUri = "https://cards.scryfall.io/normal/front/d/7/d7e36efa-266f-48f1-b352-862920487f58.jpg?1783943000",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

val Lure10eReprintB = Printing(
    oracleId = "7a7425ba-4478-4bc4-855f-abf947ea4fa2",
    name = "Lure",
    setCode = "10E",
    collectorNumber = "276★",
    scryfallId = "a07260aa-96f4-440f-b448-72482a5643b1",
    artist = "D. Alexander Gregory",
    imageUri = "https://cards.scryfall.io/normal/front/a/0/a07260aa-96f4-440f-b448-72482a5643b1.jpg?1783942999",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)

package com.wingedsheep.mtg.sets.definitions.ice.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lure reprint in ICE.
 * Canonical CardDefinition lives in Limited Edition Alpha (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.lea.cards.Lure`.
 */
val LureIceReprint = Printing(
    oracleId = "7a7425ba-4478-4bc4-855f-abf947ea4fa2",
    name = "Lure",
    setCode = "ICE",
    collectorNumber = "253",
    scryfallId = "87af69ee-c2bb-46ea-8d36-d484d04a3c8a",
    artist = "Phil Foglio",
    imageUri = "https://cards.scryfall.io/normal/front/8/7/87af69ee-c2bb-46ea-8d36-d484d04a3c8a.jpg?1783947475",
    releaseDate = "1995-06-03",
    rarity = Rarity.UNCOMMON,
)

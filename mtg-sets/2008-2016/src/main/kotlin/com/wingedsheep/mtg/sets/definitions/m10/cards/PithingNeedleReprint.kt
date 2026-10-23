package com.wingedsheep.mtg.sets.definitions.m10.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Pithing Needle reprint in M10. Canonical CardDefinition lives in SOK (the earliest real printing);
 * this file contributes only the M10 presentation row, picked up automatically by
 * `CardDiscovery.findPrintingsIn`.
 */
val PithingNeedleReprint = Printing(
    oracleId = "a188fe7e-68de-4c7c-806c-bfe8fc7b44bf",
    name = "Pithing Needle",
    setCode = "M10",
    collectorNumber = "217",
    scryfallId = "c300dcb0-1909-47d8-9818-46b2763d2bf9",
    artist = "Pete Venters",
    imageUri = "https://cards.scryfall.io/normal/front/c/3/c300dcb0-1909-47d8-9818-46b2763d2bf9.jpg?1783942355",
    releaseDate = "2009-07-17",
    rarity = Rarity.RARE,
)

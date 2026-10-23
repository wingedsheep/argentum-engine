package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Pithing Needle reprint in 10E. Canonical CardDefinition lives in SOK (the earliest real printing);
 * this file contributes only the 10E presentation row, picked up automatically by
 * `CardDiscovery.findPrintingsIn`.
 */
val PithingNeedleReprint = Printing(
    oracleId = "a188fe7e-68de-4c7c-806c-bfe8fc7b44bf",
    name = "Pithing Needle",
    setCode = "10E",
    collectorNumber = "338",
    scryfallId = "79d8adbf-37b8-4d69-b25a-57ebd777b53d",
    artist = "Pete Venters",
    imageUri = "https://cards.scryfall.io/normal/front/7/9/79d8adbf-37b8-4d69-b25a-57ebd777b53d.jpg?1783942981",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)

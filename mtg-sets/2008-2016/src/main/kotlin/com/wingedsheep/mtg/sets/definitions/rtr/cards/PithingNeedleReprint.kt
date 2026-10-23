package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Pithing Needle reprint in RTR. Canonical CardDefinition lives in SOK (the earliest real printing);
 * this file contributes only the RTR presentation row, picked up automatically by
 * `CardDiscovery.findPrintingsIn`.
 */
val PithingNeedleReprint = Printing(
    oracleId = "a188fe7e-68de-4c7c-806c-bfe8fc7b44bf",
    name = "Pithing Needle",
    setCode = "RTR",
    collectorNumber = "231",
    scryfallId = "786c1e91-9d75-46a3-9e0d-56d29fcb01a7",
    artist = "Anthony Palumbo",
    imageUri = "https://cards.scryfall.io/normal/front/7/8/786c1e91-9d75-46a3-9e0d-56d29fcb01a7.jpg?1783940323",
    releaseDate = "2012-10-05",
    rarity = Rarity.RARE,
)

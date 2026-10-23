package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Pithing Needle reprint in MID. Canonical CardDefinition lives in SOK (the earliest real printing);
 * this file contributes only the MID presentation row, picked up automatically by
 * `CardDiscovery.findPrintingsIn`.
 */
val PithingNeedleReprint = Printing(
    oracleId = "a188fe7e-68de-4c7c-806c-bfe8fc7b44bf",
    name = "Pithing Needle",
    setCode = "MID",
    collectorNumber = "257",
    scryfallId = "556ec6ab-a19f-4caa-8bfa-145555402caf",
    artist = "Ovidio Cartagena",
    imageUri = "https://cards.scryfall.io/normal/front/5/5/556ec6ab-a19f-4caa-8bfa-145555402caf.jpg?1783925547",
    releaseDate = "2021-09-24",
    rarity = Rarity.RARE,
)

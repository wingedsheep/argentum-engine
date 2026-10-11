package com.wingedsheep.mtg.sets.definitions.c16.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Kalonian Hydra reprint in Commander 2016. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the `m14` `cards/` package
 * (the card's earliest real printing); this file contributes only per-printing presentation data.
 */
val KalonianHydraReprint = Printing(
    oracleId = "7bd36106-04fe-481f-b16e-e076dcbb183b",
    name = "Kalonian Hydra",
    setCode = "C16",
    collectorNumber = "154",
    scryfallId = "17222e1e-879e-4b6a-a160-5443299b4951",
    artist = "Chris Rahn",
    imageUri = "https://cards.scryfall.io/normal/front/1/7/17222e1e-879e-4b6a-a160-5443299b4951.jpg?1783937058",
    releaseDate = "2016-11-11",
    rarity = Rarity.MYTHIC,
)

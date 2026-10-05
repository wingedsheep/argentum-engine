package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Not Forgotten reprint in J22. Canonical CardDefinition lives in Shadows over Innistrad (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.soi.cards.NotForgotten`.
 */
val NotForgottenReprint = Printing(
    oracleId = "e0fa6d08-1808-4a29-b1e4-5cec9d31d798",
    name = "Not Forgotten",
    setCode = "J22",
    collectorNumber = "222",
    scryfallId = "05860b72-fb1b-44d8-9275-12863724a9ef",
    artist = "Darek Zabrocki",
    imageUri = "https://cards.scryfall.io/normal/front/0/5/05860b72-fb1b-44d8-9275-12863724a9ef.jpg?1783919097",
    releaseDate = "2022-12-02",
    rarity = Rarity.UNCOMMON,
)

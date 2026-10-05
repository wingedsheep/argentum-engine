package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Drownyard Explorers reprint in J22. Canonical CardDefinition lives in Shadows over Innistrad (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.soi.cards.DrownyardExplorers`.
 */
val DrownyardExplorersReprint = Printing(
    oracleId = "c1ebbe89-8976-4ed7-a68a-8b08aac4616c",
    name = "Drownyard Explorers",
    setCode = "J22",
    collectorNumber = "289",
    scryfallId = "aa7a4792-0d24-47d7-a1df-818d97d40904",
    artist = "Anthony Palumbo",
    imageUri = "https://cards.scryfall.io/normal/front/a/a/aa7a4792-0d24-47d7-a1df-818d97d40904.jpg?1783919066",
    releaseDate = "2022-12-02",
    rarity = Rarity.COMMON,
)

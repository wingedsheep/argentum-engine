package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dread Slaver reprint in J22. Canonical CardDefinition lives in Avacyn Restored (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.avr.cards.DreadSlaver`.
 */
val DreadSlaverReprint = Printing(
    oracleId = "8110f092-41b0-4e53-a7d7-f7e3cbc4a52a",
    name = "Dread Slaver",
    setCode = "J22",
    collectorNumber = "405",
    scryfallId = "6b84820d-187c-4d3b-b7a7-7999d4efe443",
    artist = "Dave Kendall",
    imageUri = "https://cards.scryfall.io/normal/front/6/b/6b84820d-187c-4d3b-b7a7-7999d4efe443.jpg?1783919013",
    releaseDate = "2022-12-02",
    rarity = Rarity.RARE,
)

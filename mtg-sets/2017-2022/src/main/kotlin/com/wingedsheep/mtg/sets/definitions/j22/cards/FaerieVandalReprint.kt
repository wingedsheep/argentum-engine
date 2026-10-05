package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Faerie Vandal reprint in J22. Canonical CardDefinition lives in Throne of Eldraine (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.eld.cards.FaerieVandal`.
 */
val FaerieVandalReprint = Printing(
    oracleId = "b872f147-00eb-4cf8-af8c-4144356b2089",
    name = "Faerie Vandal",
    setCode = "J22",
    collectorNumber = "296",
    scryfallId = "bc487c86-caae-4bc0-b41b-5227404e761f",
    artist = "Paul Scott Canavan",
    imageUri = "https://cards.scryfall.io/normal/front/b/c/bc487c86-caae-4bc0-b41b-5227404e761f.jpg?1783919062",
    releaseDate = "2022-12-02",
    rarity = Rarity.UNCOMMON,
)

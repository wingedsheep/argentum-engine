package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Web Up reprint in Marvel Super Heroes. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in another set's `cards/` package
 * (`definitions/spm/cards/WebUp.kt`); this file contributes only presentation data.
 */
val WebUpReprint = Printing(
    oracleId = "9b9b086c-d201-4574-a1e9-0d6f8de53d7d",
    name = "Web Up",
    setCode = "MSH",
    collectorNumber = "41",
    scryfallId = "9a1b057b-229c-4f65-ba4e-12dd342238de",
    artist = "Nathaniel Himawan",
    imageUri = "https://cards.scryfall.io/normal/front/9/a/9a1b057b-229c-4f65-ba4e-12dd342238de.jpg?1783902964",
    releaseDate = "2026-06-26",
    rarity = Rarity.COMMON,
)

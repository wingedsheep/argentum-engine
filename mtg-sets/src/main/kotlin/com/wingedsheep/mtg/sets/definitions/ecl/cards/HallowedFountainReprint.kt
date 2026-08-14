package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Hallowed Fountain reprint in Lorwyn Eclipsed. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Dissension (`dis`) `cards/` package
 * (the card's earliest real printing); this file contributes only per-printing presentation data.
 */
val HallowedFountainReprint = Printing(
    oracleId = "f1750962-a87c-49f6-b731-02ae971ac6ea",
    name = "Hallowed Fountain",
    setCode = "ECL",
    collectorNumber = "265",
    scryfallId = "e056b55f-82ed-4fe0-ab0c-bb20fa4a218a",
    artist = "Adam Paquette",
    imageUri = "https://cards.scryfall.io/normal/front/e/0/e056b55f-82ed-4fe0-ab0c-bb20fa4a218a.jpg?1759144845",
    releaseDate = "2026-01-23",
    rarity = Rarity.RARE,
)

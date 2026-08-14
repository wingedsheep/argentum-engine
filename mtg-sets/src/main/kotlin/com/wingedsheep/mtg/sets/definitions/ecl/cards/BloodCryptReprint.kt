package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Blood Crypt reprint in Lorwyn Eclipsed. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Dissension (`dis`) `cards/` package
 * (the card's earliest real printing); this file contributes only per-printing presentation data.
 */
val BloodCryptReprint = Printing(
    oracleId = "43985bbc-a0f6-4812-984e-392bc8562633",
    name = "Blood Crypt",
    setCode = "ECL",
    collectorNumber = "262",
    scryfallId = "6da63cc5-4624-4491-abd9-9b600c3fefe2",
    artist = "Adam Paquette",
    imageUri = "https://cards.scryfall.io/normal/front/6/d/6da63cc5-4624-4491-abd9-9b600c3fefe2.jpg?1759144844",
    releaseDate = "2026-01-23",
    rarity = Rarity.RARE,
)

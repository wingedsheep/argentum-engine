package com.wingedsheep.mtg.sets.definitions.msc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Tome of Legends reprints in Marvel Super Heroes Commander. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the `eld` `cards/` package (the card's
 * earliest real printing); these rows contribute only per-printing presentation data.
 */
val TomeOfLegendsReprint = Printing(
    oracleId = "cada2a1c-5db2-4702-9a57-cbe7da1bf208",
    name = "Tome of Legends",
    setCode = "MSC",
    collectorNumber = "223",
    scryfallId = "84546bb2-6c82-4e3e-8f64-cb7aa865ee60",
    artist = "Monztre",
    imageUri = "https://cards.scryfall.io/normal/front/8/4/84546bb2-6c82-4e3e-8f64-cb7aa865ee60.jpg?1783903209",
    releaseDate = "2026-06-26",
    rarity = Rarity.RARE,
)

val TomeOfLegendsVariantReprint = Printing(
    oracleId = "cada2a1c-5db2-4702-9a57-cbe7da1bf208",
    name = "Tome of Legends",
    setCode = "MSC",
    collectorNumber = "454",
    scryfallId = "3a50a89f-aca9-409e-b6b5-dbe3a361b96f",
    artist = "Monztre",
    imageUri = "https://cards.scryfall.io/normal/front/3/a/3a50a89f-aca9-409e-b6b5-dbe3a361b96f.jpg?1783903127",
    releaseDate = "2026-06-26",
    rarity = Rarity.RARE,
    frameEffects = listOf("extendedart"),
)

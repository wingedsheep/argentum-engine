package com.wingedsheep.mtg.sets.definitions.msc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Toxic Deluge reprints in Marvel Super Heroes Commander. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in the `c13` `cards/` package (the card's earliest real printing); these rows contribute
 * only per-printing presentation data.
 */
val ToxicDelugeReprint = Printing(
    oracleId = "afaef788-34d1-460b-b884-9d7ae6ddeb18",
    name = "Toxic Deluge",
    setCode = "MSC",
    collectorNumber = "161",
    scryfallId = "de5afccc-8d42-4bd6-b068-b9ea2361655e",
    artist = "Anthony Devine",
    imageUri = "https://cards.scryfall.io/normal/front/d/e/de5afccc-8d42-4bd6-b068-b9ea2361655e.jpg?1783903236",
    releaseDate = "2026-06-26",
    rarity = Rarity.RARE,
)

val ToxicDelugeVariantReprint = Printing(
    oracleId = "afaef788-34d1-460b-b884-9d7ae6ddeb18",
    name = "Toxic Deluge",
    setCode = "MSC",
    collectorNumber = "354",
    scryfallId = "ab3ab559-5e16-44d9-97be-a4251c6e2b94",
    artist = "Anthony Devine",
    imageUri = "https://cards.scryfall.io/normal/front/a/b/ab3ab559-5e16-44d9-97be-a4251c6e2b94.jpg?1783903162",
    releaseDate = "2026-06-26",
    rarity = Rarity.RARE,
    frameEffects = listOf("extendedart"),
)

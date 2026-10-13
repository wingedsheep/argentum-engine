package com.wingedsheep.mtg.sets.definitions.msc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Plaza of Heroes reprints in Marvel Super Heroes Commander. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in the `dmu` `cards/` package (the card's earliest real printing); these rows contribute
 * only per-printing presentation data.
 */
val PlazaOfHeroesReprint = Printing(
    oracleId = "9c58d241-4d9f-4b46-b8ee-f4587f9acfd6",
    name = "Plaza of Heroes",
    setCode = "MSC",
    collectorNumber = "255",
    scryfallId = "96fe4b9b-d766-463b-a6df-345ebebfc17c",
    artist = "Taurin Clarke",
    imageUri = "https://cards.scryfall.io/normal/front/9/6/96fe4b9b-d766-463b-a6df-345ebebfc17c.jpg?1783903197",
    releaseDate = "2026-06-26",
    rarity = Rarity.RARE,
)

val PlazaOfHeroesVariantReprint = Printing(
    oracleId = "9c58d241-4d9f-4b46-b8ee-f4587f9acfd6",
    name = "Plaza of Heroes",
    setCode = "MSC",
    collectorNumber = "483",
    scryfallId = "2375d88b-b49f-43b2-a4cd-bfa2c9faa879",
    artist = "Taurin Clarke",
    imageUri = "https://cards.scryfall.io/normal/front/2/3/2375d88b-b49f-43b2-a4cd-bfa2c9faa879.jpg?1783903121",
    releaseDate = "2026-06-26",
    rarity = Rarity.RARE,
    frameEffects = listOf("extendedart"),
)

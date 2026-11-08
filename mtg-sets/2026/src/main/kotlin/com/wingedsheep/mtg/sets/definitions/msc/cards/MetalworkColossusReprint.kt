package com.wingedsheep.mtg.sets.definitions.msc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Metalwork Colossus reprints in Marvel Super Heroes Commander. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in the `kld` `cards/` package (the card's earliest real printing); this file contributes
 * only per-printing presentation data.
 */
val MetalworkColossusReprint = Printing(
    oracleId = "64ffe529-ba53-48ec-8af1-b046fbd1f0cc",
    name = "Metalwork Colossus",
    setCode = "MSC",
    collectorNumber = "204",
    scryfallId = "d7e2d686-1fbc-449a-9af4-f00a71ff7911",
    artist = "Josu Hernaiz",
    imageUri = "https://cards.scryfall.io/normal/front/d/7/d7e2d686-1fbc-449a-9af4-f00a71ff7911.jpg?1783903217",
    releaseDate = "2026-06-26",
    rarity = Rarity.RARE,
)

val MetalworkColossusVariantReprint = Printing(
    oracleId = "64ffe529-ba53-48ec-8af1-b046fbd1f0cc",
    name = "Metalwork Colossus",
    setCode = "MSC",
    collectorNumber = "442",
    scryfallId = "82d868c2-aa72-4141-b508-9ff13c76ec4b",
    artist = "Josu Hernaiz",
    imageUri = "https://cards.scryfall.io/normal/front/8/2/82d868c2-aa72-4141-b508-9ff13c76ec4b.jpg?1783903131",
    releaseDate = "2026-06-26",
    rarity = Rarity.RARE,
    frameEffects = listOf("extendedart"),
)

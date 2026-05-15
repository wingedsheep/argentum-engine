package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Defiant Strike reprint in WAR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * an earlier set's `cards/` package. This file contributes only the WAR-specific
 * presentation row.
 */
val DefiantStrikeReprint = Printing(
    oracleId = "f8c2d9c5-6fe6-4698-9972-83cdd625d94b",
    name = "Defiant Strike",
    setCode = "WAR",
    collectorNumber = "9",
    scryfallId = "dc183a40-7e15-40b1-9faa-b243401b3d10",
    artist = "Gabor Szikszai",
    imageUri = "https://cards.scryfall.io/normal/front/d/c/dc183a40-7e15-40b1-9faa-b243401b3d10.jpg?1557575911",
    releaseDate = "2019-05-03",
    rarity = Rarity.COMMON,
)

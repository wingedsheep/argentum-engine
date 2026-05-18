package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Chromatic Sphere reprint in MRD.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * Invasion's `cards/` package. This file contributes only the MRD-specific
 * presentation row.
 */
val ChromaticSphereReprint = Printing(
    oracleId = "2e03e44a-9fff-4490-859f-b42e89e8563a",
    name = "Chromatic Sphere",
    setCode = "MRD",
    collectorNumber = "151",
    scryfallId = "f31a6dfd-93d2-49c8-a357-9a707b9c42bd",
    artist = "Brian Snõddy",
    imageUri = "https://cards.scryfall.io/normal/front/f/3/f31a6dfd-93d2-49c8-a357-9a707b9c42bd.jpg?1592471872",
    releaseDate = "2003-10-02",
    rarity = Rarity.COMMON,
)

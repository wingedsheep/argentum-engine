package com.wingedsheep.mtg.sets.definitions.sld.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Blood Artist reprint in SLD. Canonical CardDefinition lives in its earliest set.
 */
val BloodArtistReprint = Printing(
    oracleId = "310f141c-7f37-4729-aed6-dd9c09db448d",
    name = "Blood Artist",
    setCode = "SLD",
    collectorNumber = "42",
    scryfallId = "8a5b65ed-250c-42a6-84c0-ac06662ca5ed",
    artist = "Joshua Howard",
    imageUri = "https://cards.scryfall.io/normal/front/8/a/8a5b65ed-250c-42a6-84c0-ac06662ca5ed.jpg",
    releaseDate = "2020-06-01",
    rarity = Rarity.RARE,
)

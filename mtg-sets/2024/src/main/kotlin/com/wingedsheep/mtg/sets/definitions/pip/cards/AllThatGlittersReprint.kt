package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * All That Glitters reprint in PIP. Canonical CardDefinition lives in Throne of Eldraine (its
 * earliest real printing), `com.wingedsheep.mtg.sets.definitions.eld.cards.AllThatGlitters`.
 */
val AllThatGlittersReprint = Printing(
    oracleId = "a4d751e0-41c1-4e90-853d-512f385acd81",
    name = "All That Glitters",
    setCode = "PIP",
    collectorNumber = "155",
    scryfallId = "15d9fb74-9f2d-40f1-88b8-514a460a4f86",
    artist = "Kieran Yanner",
    imageUri = "https://cards.scryfall.io/normal/front/1/5/15d9fb74-9f2d-40f1-88b8-514a460a4f86.jpg?1783912372",
    releaseDate = "2024-03-08",
    rarity = Rarity.COMMON,
)

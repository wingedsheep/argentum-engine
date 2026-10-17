package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mantle of the Ancients reprint in PIP. Canonical CardDefinition lives in Forgotten Realms
 * Commander (its earliest real printing), `com.wingedsheep.mtg.sets.definitions.afc.cards.MantleOfTheAncients`.
 */
val MantleOfTheAncientsReprint = Printing(
    oracleId = "7424560f-557f-4bc9-a3e7-eb890c73aaa3",
    name = "Mantle of the Ancients",
    setCode = "PIP",
    collectorNumber = "165",
    scryfallId = "56ec4350-05fa-4452-ade3-3cf2c319eb0b",
    artist = "Igor Krstic",
    imageUri = "https://cards.scryfall.io/normal/front/5/6/56ec4350-05fa-4452-ade3-3cf2c319eb0b.jpg?1783912368",
    releaseDate = "2024-03-08",
    rarity = Rarity.RARE,
)

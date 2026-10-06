package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Might of the Masses reprint in GRN. Canonical CardDefinition lives in Rise of the Eldrazi (its earliest
 * real printing), `com.wingedsheep.mtg.sets.definitions.roe.cards.MightOfTheMasses`.
 */
val MightOfTheMassesReprint = Printing(
    oracleId = "f73416a8-40e5-4791-948f-147ef2221ee7",
    name = "Might of the Masses",
    setCode = "GRN",
    collectorNumber = "137",
    scryfallId = "cf6d9ef7-0b16-4095-9c70-f6776e4cd3cb",
    artist = "Alex Konstad",
    imageUri = "https://cards.scryfall.io/normal/front/c/f/cf6d9ef7-0b16-4095-9c70-f6776e4cd3cb.jpg?1783934149",
    releaseDate = "2018-10-05",
    rarity = Rarity.UNCOMMON,
)

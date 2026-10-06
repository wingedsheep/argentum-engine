package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Might of the Masses reprint in ORI. Canonical CardDefinition lives in Rise of the Eldrazi (its earliest
 * real printing), `com.wingedsheep.mtg.sets.definitions.roe.cards.MightOfTheMasses`.
 */
val MightOfTheMassesReprint = Printing(
    oracleId = "f73416a8-40e5-4791-948f-147ef2221ee7",
    name = "Might of the Masses",
    setCode = "ORI",
    collectorNumber = "188",
    scryfallId = "c3b53cc0-ef96-40da-b9b8-d93fdae40cb8",
    artist = "Johann Bodin",
    imageUri = "https://cards.scryfall.io/normal/front/c/3/c3b53cc0-ef96-40da-b9b8-d93fdae40cb8.jpg?1783938320",
    releaseDate = "2015-07-17",
    rarity = Rarity.COMMON,
)

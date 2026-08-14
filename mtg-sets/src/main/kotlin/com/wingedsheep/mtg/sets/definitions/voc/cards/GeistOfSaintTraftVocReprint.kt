package com.wingedsheep.mtg.sets.definitions.voc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Geist of Saint Traft reprint in VOC. Canonical CardDefinition lives in Innistrad (its earliest real
 * printing), `com.wingedsheep.mtg.sets.definitions.isd.cards.GeistOfSaintTraft`.
 */
val GeistOfSaintTraftVocReprint = Printing(
    oracleId = "4304035c-c3e4-4a19-aa1f-92a83d8aed1f",
    name = "Geist of Saint Traft",
    setCode = "VOC",
    collectorNumber = "155",
    scryfallId = "541b9d55-f237-4ff2-9e47-c58a381f0633",
    artist = "Igor Kieryluk",
    imageUri = "https://cards.scryfall.io/normal/front/5/4/541b9d55-f237-4ff2-9e47-c58a381f0633.jpg?1783924944",
    releaseDate = "2021-11-19",
    rarity = Rarity.MYTHIC,
)

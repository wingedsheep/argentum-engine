package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Malakir Blood-Priest reprint in CLB. The canonical CardDefinition lives in
 * Zendikar Rising (`znr`), the card's earliest real printing; this file
 * contributes only per-printing presentation data.
 */
val MalakirBloodPriestReprint = Printing(
    oracleId = "3a3cb6fe-24ef-4c09-8ff8-f303fc472a8b",
    name = "Malakir Blood-Priest",
    setCode = "CLB",
    collectorNumber = "760",
    scryfallId = "bbbd64da-6071-49f4-87aa-066209ec1ae9",
    artist = "Scott Murphy",
    imageUri = "https://cards.scryfall.io/normal/front/b/b/bbbd64da-6071-49f4-87aa-066209ec1ae9.jpg?1783922447",
    releaseDate = "2022-06-10",
    rarity = Rarity.COMMON,
)

package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Demonic Gifts reprint in J22. Canonical CardDefinition lives in Kaldheim (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.khm.cards.DemonicGifts`.
 */
val DemonicGiftsReprint = Printing(
    oracleId = "162bcc9d-6edf-4b3b-83e6-2c933e2a5967",
    name = "Demonic Gifts",
    setCode = "J22",
    collectorNumber = "398",
    scryfallId = "6721bbf0-b268-4030-aa90-713f0e9360c5",
    artist = "Kekai Kotaki",
    imageUri = "https://cards.scryfall.io/normal/front/6/7/6721bbf0-b268-4030-aa90-713f0e9360c5.jpg?1783919014",
    releaseDate = "2022-12-02",
    rarity = Rarity.COMMON,
)

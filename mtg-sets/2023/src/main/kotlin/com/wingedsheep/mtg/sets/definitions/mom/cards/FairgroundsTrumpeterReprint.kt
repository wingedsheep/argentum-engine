package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fairgrounds Trumpeter reprint in MOM.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in Kaladesh (`kld`). This file
 * contributes only the MOM-specific presentation row — set, collector number, art.
 */
val FairgroundsTrumpeterReprint = Printing(
    oracleId = "3dfc6a52-d58c-4455-96fe-ec193743d67a",
    name = "Fairgrounds Trumpeter",
    setCode = "MOM",
    collectorNumber = "335",
    scryfallId = "f2c6408b-9e24-4bf5-b0d0-a4cda3069b1c",
    artist = "Samuel Perin",
    imageUri = "https://cards.scryfall.io/normal/front/f/2/f2c6408b-9e24-4bf5-b0d0-a4cda3069b1c.jpg?1783916901",
    releaseDate = "2023-04-21",
    rarity = Rarity.COMMON,
)

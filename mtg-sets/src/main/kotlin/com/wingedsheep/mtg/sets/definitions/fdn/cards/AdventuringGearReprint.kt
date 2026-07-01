package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Adventuring Gear reprint in FDN. Canonical [com.wingedsheep.sdk.model.CardDefinition] lives in
 * Zendikar (ZEN); this file contributes only the FDN presentation row.
 */
val AdventuringGearReprint = Printing(
    oracleId = "17042684-7af4-43a6-88c7-885032cfb27c",
    name = "Adventuring Gear",
    setCode = "FDN",
    collectorNumber = "249",
    scryfallId = "361f9b99-5b5d-40da-b4b9-5ad90f6280ee",
    artist = "Howard Lyon",
    imageUri = "https://cards.scryfall.io/normal/front/3/6/361f9b99-5b5d-40da-b4b9-5ad90f6280ee.jpg?1782689053",
    releaseDate = "2024-11-15",
    rarity = Rarity.UNCOMMON,
)

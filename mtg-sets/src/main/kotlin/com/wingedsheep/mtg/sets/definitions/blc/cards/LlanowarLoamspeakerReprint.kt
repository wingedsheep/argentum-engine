package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Llanowar Loamspeaker reprint in BLC.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in
 * `definitions/dmu/cards/LlanowarLoamspeaker.kt`. This file contributes only the
 * BLC-specific presentation row.
 */
val LlanowarLoamspeakerReprint = Printing(
    oracleId = "08377424-619e-48ae-bc3c-fb199cd77cf9",
    name = "Llanowar Loamspeaker",
    setCode = "BLC",
    collectorNumber = "228",
    scryfallId = "0dba8ffb-b7b1-411e-90c5-070b7a888e3f",
    artist = "Zara Alfonso",
    imageUri = "https://cards.scryfall.io/normal/front/0/d/0dba8ffb-b7b1-411e-90c5-070b7a888e3f.jpg?1721429323",
    releaseDate = "2024-08-02",
    rarity = Rarity.RARE,
)

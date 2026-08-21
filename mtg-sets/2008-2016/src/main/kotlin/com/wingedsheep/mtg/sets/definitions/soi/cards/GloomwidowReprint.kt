package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Gloomwidow reprint in Shadows over Innistrad. The canonical CardDefinition lives in Shadowmoor
 * (`definitions/shm/cards/Gloomwidow.kt`); this file contributes only per-printing presentation data.
 */
val GloomwidowReprint = Printing(
    oracleId = "e5139376-cdb1-4f1e-b417-b956d6b713b9",
    name = "Gloomwidow",
    setCode = "SOI",
    collectorNumber = "206",
    scryfallId = "ee04dfd8-e704-46d7-bdf8-b0b2ee747a49",
    artist = "Svetlin Velinov",
    imageUri = "https://cards.scryfall.io/normal/front/e/e/ee04dfd8-e704-46d7-bdf8-b0b2ee747a49.jpg?1783937730",
    releaseDate = "2016-04-08",
    rarity = Rarity.UNCOMMON,
)

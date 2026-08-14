package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Gearseeker Serpent reprint in J22.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in
 * `definitions/kld/cards/GearseekerSerpent.kt` — Kaladesh (2016) is the card's earliest real
 * expansion printing. This file contributes only the J22-specific presentation row.
 */
val GearseekerSerpentReprint = Printing(
    oracleId = "fdbd8a95-1dc8-4df2-bab0-a93d1941a405",
    name = "Gearseeker Serpent",
    setCode = "J22",
    collectorNumber = "302",
    scryfallId = "8d3ff91c-3d6c-45ed-bc58-28b17e8a213d",
    artist = "Filip Burburan",
    imageUri = "https://cards.scryfall.io/normal/front/8/d/8d3ff91c-3d6c-45ed-bc58-28b17e8a213d.jpg?1783919058",
    releaseDate = "2022-12-02",
    rarity = Rarity.COMMON,
)

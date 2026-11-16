package com.wingedsheep.mtg.sets.definitions.sld.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dragonlord Silumgar reprint in SLD. Canonical CardDefinition lives in its earliest set (DTK).
 * Scryfall lists this printing as a reversible card; both faces are the same card, so only the
 * front face's art is used.
 */
val DragonlordSilumgarReprint = Printing(
    oracleId = "44581d4f-746b-43b8-a34d-16dc383ab70e",
    name = "Dragonlord Silumgar",
    setCode = "SLD",
    collectorNumber = "1974",
    scryfallId = "4e82ad5e-6c7c-4140-bbef-9c97b7765e4e",
    artist = "Alison Johnstun",
    imageUri = "https://cards.scryfall.io/normal/front/4/e/4e82ad5e-6c7c-4140-bbef-9c97b7765e4e.jpg?1783907454",
    releaseDate = "2025-03-24",
    rarity = Rarity.MYTHIC,
)

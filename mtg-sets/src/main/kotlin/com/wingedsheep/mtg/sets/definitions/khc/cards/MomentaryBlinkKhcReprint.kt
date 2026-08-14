package com.wingedsheep.mtg.sets.definitions.khc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Momentary Blink reprint in KHC. Canonical CardDefinition lives in Time Spiral (its earliest real
 * printing), `com.wingedsheep.mtg.sets.definitions.tsp.cards.MomentaryBlink`.
 */
val MomentaryBlinkKhcReprint = Printing(
    oracleId = "a3ec6b5d-08ec-4ae0-b1db-c4b87a1849c7",
    name = "Momentary Blink",
    setCode = "KHC",
    collectorNumber = "30",
    scryfallId = "ee164002-0416-4232-9484-2e15d8afd6d4",
    artist = "Evan Shipard",
    imageUri = "https://cards.scryfall.io/normal/front/e/e/ee164002-0416-4232-9484-2e15d8afd6d4.jpg?1783928329",
    releaseDate = "2021-02-05",
    rarity = Rarity.COMMON,
)

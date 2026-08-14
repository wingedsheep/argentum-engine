package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Gearseeker Serpent reprint in DFT.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in
 * `definitions/kld/cards/GearseekerSerpent.kt` — Kaladesh (2016) is the card's earliest real
 * expansion printing. This file contributes only the DFT-specific presentation row — set,
 * collector number, art — picked up automatically by `CardDiscovery.findPrintingsIn`.
 */
val GearseekerSerpentReprint = Printing(
    oracleId = "fdbd8a95-1dc8-4df2-bab0-a93d1941a405",
    name = "Gearseeker Serpent",
    setCode = "DFT",
    collectorNumber = "43",
    scryfallId = "3dca0007-42d3-4ee2-8e88-361d80a7103c",
    artist = "J.P. Targete",
    imageUri = "https://cards.scryfall.io/normal/front/3/d/3dca0007-42d3-4ee2-8e88-361d80a7103c.jpg?1783907909",
    releaseDate = "2025-02-14",
    rarity = Rarity.COMMON,
)

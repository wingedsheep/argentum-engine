package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Kraven the Hunter reprint in SPM (Marvel's Spider-Man).
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in the OM1 package
 * (Through the Omenpaths, the card's earliest real-expansion printing). This file
 * contributes only the SPM-specific presentation row.
 */
val KravenTheHunterReprint = Printing(
    oracleId = "3e2bfa3a-ae83-453a-8e3f-ca6205a9af12",
    name = "Kraven the Hunter",
    setCode = "SPM",
    collectorNumber = "133",
    scryfallId = "afdab464-3674-449b-be01-1cbd21fced23",
    artist = "Greg Staples",
    imageUri = "https://cards.scryfall.io/normal/front/a/f/afdab464-3674-449b-be01-1cbd21fced23.jpg?1757377713",
    releaseDate = "2025-09-26",
    rarity = Rarity.RARE,
)

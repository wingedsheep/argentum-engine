package com.wingedsheep.mtg.sets.definitions.dsc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sandwurm Convergence reprint in DSC. The canonical CardDefinition lives in
 * Amonkhet (`akh`), the card's earliest real printing; this file
 * contributes only per-printing presentation data.
 */
val SandwurmConvergenceReprint = Printing(
    oracleId = "429eff2b-635b-43f6-b20f-82e25992c0b9",
    name = "Sandwurm Convergence",
    setCode = "DSC",
    collectorNumber = "195",
    scryfallId = "3ad9cb61-4072-449b-9b32-83e2192cb292",
    artist = "Slawomir Maniak",
    imageUri = "https://cards.scryfall.io/normal/front/3/a/3ad9cb61-4072-449b-9b32-83e2192cb292.jpg?1783909601",
    releaseDate = "2024-09-27",
    rarity = Rarity.RARE,
)

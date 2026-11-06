package com.wingedsheep.mtg.sets.definitions.ncc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sandwurm Convergence reprint in NCC. The canonical CardDefinition lives in
 * Amonkhet (`akh`), the card's earliest real printing; this file
 * contributes only per-printing presentation data.
 */
val SandwurmConvergenceReprint = Printing(
    oracleId = "429eff2b-635b-43f6-b20f-82e25992c0b9",
    name = "Sandwurm Convergence",
    setCode = "NCC",
    collectorNumber = "308",
    scryfallId = "fdd90a01-c466-47f4-9229-7bf5cbac8cea",
    artist = "Slawomir Maniak",
    imageUri = "https://cards.scryfall.io/normal/front/f/d/fdd90a01-c466-47f4-9229-7bf5cbac8cea.jpg?1783923241",
    releaseDate = "2022-04-29",
    rarity = Rarity.RARE,
)

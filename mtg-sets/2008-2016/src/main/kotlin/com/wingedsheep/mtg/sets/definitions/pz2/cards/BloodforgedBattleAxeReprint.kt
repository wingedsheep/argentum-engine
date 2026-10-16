package com.wingedsheep.mtg.sets.definitions.pz2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bloodforged Battle-Axe reprint in Treasure Chest (PZ2).
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in C17's `cards/` package (the
 * card's earliest real printing). This file contributes only the PZ2-specific presentation row.
 */
val BloodforgedBattleAxeReprint = Printing(
    oracleId = "81a7f559-5edd-47ec-91f7-51e5ee998ed0",
    name = "Bloodforged Battle-Axe",
    setCode = "PZ2",
    collectorNumber = "65713",
    scryfallId = "d9a5219e-f0f3-4fbe-b162-9a03c7019056",
    artist = "Alayna Danner",
    imageUri = "https://cards.scryfall.io/normal/front/d/9/d9a5219e-f0f3-4fbe-b162-9a03c7019056.jpg?1783935582",
    releaseDate = "2017-11-15",
    rarity = Rarity.RARE,
)

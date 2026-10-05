package com.wingedsheep.mtg.sets.definitions.pz2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Balan, Wandering Knight reprint in Treasure Chest (PZ2).
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in C17's `cards/` package (the
 * card's earliest real printing). This file contributes only the PZ2-specific presentation row.
 */
val BalanWanderingKnightReprint = Printing(
    oracleId = "d12cfd46-73c2-4206-9a10-0d2e7d6ac87e",
    name = "Balan, Wandering Knight",
    setCode = "PZ2",
    collectorNumber = "65757",
    scryfallId = "70b202fc-3846-4585-a68a-74492e2ce9fb",
    artist = "Svetlin Velinov",
    imageUri = "https://cards.scryfall.io/normal/front/7/0/70b202fc-3846-4585-a68a-74492e2ce9fb.jpg?1783935569",
    releaseDate = "2017-11-15",
    rarity = Rarity.RARE,
)

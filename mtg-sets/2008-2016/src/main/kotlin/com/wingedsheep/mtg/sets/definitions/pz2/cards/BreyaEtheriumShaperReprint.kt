package com.wingedsheep.mtg.sets.definitions.pz2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Breya, Etherium Shaper reprint in Treasure Chest (PZ2).
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in C16's `cards/` package (the
 * card's earliest real printing). This file contributes only the PZ2-specific presentation row.
 */
val BreyaEtheriumShaperReprint = Printing(
    oracleId = "d460a9e2-5a7d-4562-880e-45174be19a9d",
    name = "Breya, Etherium Shaper",
    setCode = "PZ2",
    collectorNumber = "65",
    scryfallId = "be18ced5-c78a-43d2-a6ad-9b6935358b7f",
    artist = "Clint Cearley",
    imageUri = "https://cards.scryfall.io/normal/front/b/e/be18ced5-c78a-43d2-a6ad-9b6935358b7f.jpg?1783936989",
    releaseDate = "2016-11-16",
    rarity = Rarity.MYTHIC,
)

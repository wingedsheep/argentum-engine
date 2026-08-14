package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mirrorwing Dragon reprint in INR. Canonical CardDefinition lives in EMN's `cards/` package
 * (the card's earliest real printing); this file contributes only presentation data.
 */
val MirrorwingDragonReprint = Printing(
    oracleId = "9ea14a03-c6a2-49ec-953d-4249a2c27e1d",
    name = "Mirrorwing Dragon",
    setCode = "INR",
    collectorNumber = "165",
    scryfallId = "409aa60d-ce4f-4019-b5b9-702ab94ed429",
    artist = "Min Yum",
    imageUri = "https://cards.scryfall.io/normal/front/4/0/409aa60d-ce4f-4019-b5b9-702ab94ed429.jpg?1783908114",
    releaseDate = "2025-01-24",
    rarity = Rarity.MYTHIC,
)

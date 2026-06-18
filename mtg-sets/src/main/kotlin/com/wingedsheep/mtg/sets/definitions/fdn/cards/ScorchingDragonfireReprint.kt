package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Scorching Dragonfire reprint in Foundations. Canonical CardDefinition lives in the ELD
 * `cards/` package (earliest real expansion printing); this file contributes only presentation data.
 */
val ScorchingDragonfireReprint = Printing(
    oracleId = "d14f313c-fea6-49c4-8197-5b74ee584a6b",
    name = "Scorching Dragonfire",
    setCode = "FDN",
    collectorNumber = "545",
    scryfallId = "44ea09c1-d420-4e99-a968-ade614579287",
    artist = "Eric Velhagen",
    imageUri = "https://cards.scryfall.io/normal/front/4/4/44ea09c1-d420-4e99-a968-ade614579287.jpg?1730490669",
    releaseDate = "2024-11-15",
    rarity = Rarity.COMMON,
)

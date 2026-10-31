package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Leyline of Sanctity reprint in Core Set 2020 (M20 #26). The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Magic 2011's `cards/` package
 * ([com.wingedsheep.mtg.sets.definitions.m11.cards.LeylineOfSanctity], the earliest real-expansion
 * printing); this file contributes only M20 presentation data.
 */
val LeylineOfSanctityReprint = Printing(
    oracleId = "492e0e6c-8c27-4376-938b-f8a8b6205810",
    name = "Leyline of Sanctity",
    setCode = "M20",
    collectorNumber = "26",
    scryfallId = "be8b1acf-dd87-42ca-ad19-c27d21066030",
    artist = "Noah Bradley",
    imageUri = "https://cards.scryfall.io/normal/front/b/e/be8b1acf-dd87-42ca-ad19-c27d21066030.jpg?1783933025",
    releaseDate = "2019-07-12",
    rarity = Rarity.RARE,
)

package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Thirst for Knowledge reprint in Commander Legends. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Mirrodin's (`mrd`) `cards/` package (the
 * card's earliest real printing); this file contributes only the CMR presentation row.
 */
val ThirstForKnowledgeReprint = Printing(
    oracleId = "939e6f71-185e-41f2-9d54-72cce06f1dce",
    name = "Thirst for Knowledge",
    setCode = "CMR",
    collectorNumber = "103",
    scryfallId = "1d8173ac-bd9c-4748-9a6a-e5556d74d754",
    artist = "Anthony Francisco",
    imageUri = "https://cards.scryfall.io/normal/front/1/d/1d8173ac-bd9c-4748-9a6a-e5556d74d754.jpg?1783928848",
    releaseDate = "2020-11-20",
    rarity = Rarity.UNCOMMON,
)

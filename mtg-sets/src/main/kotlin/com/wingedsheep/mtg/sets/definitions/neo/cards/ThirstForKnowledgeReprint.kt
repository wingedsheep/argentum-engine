package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Thirst for Knowledge reprint in Kamigawa: Neon Dynasty. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Mirrodin's (`mrd`) `cards/` package (the
 * card's earliest real printing); this file contributes only the NEO presentation row.
 */
val ThirstForKnowledgeReprint = Printing(
    oracleId = "939e6f71-185e-41f2-9d54-72cce06f1dce",
    name = "Thirst for Knowledge",
    setCode = "NEO",
    collectorNumber = "85",
    scryfallId = "6b927837-8252-4ea7-b2d0-ab624de65bd7",
    artist = "Anna Pavleeva",
    imageUri = "https://cards.scryfall.io/normal/front/6/b/6b927837-8252-4ea7-b2d0-ab624de65bd7.jpg?1783923893",
    releaseDate = "2022-02-18",
    rarity = Rarity.UNCOMMON,
)

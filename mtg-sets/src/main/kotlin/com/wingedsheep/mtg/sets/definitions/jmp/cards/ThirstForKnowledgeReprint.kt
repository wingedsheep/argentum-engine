package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Thirst for Knowledge reprint in Jumpstart. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Mirrodin's (`mrd`) `cards/` package (the
 * card's earliest real printing); this file contributes only the JMP presentation row.
 */
val ThirstForKnowledgeReprint = Printing(
    oracleId = "939e6f71-185e-41f2-9d54-72cce06f1dce",
    name = "Thirst for Knowledge",
    setCode = "JMP",
    collectorNumber = "183",
    scryfallId = "52e40cb2-d306-4c8d-859b-ac288e9dc78d",
    artist = "Anthony Francisco",
    imageUri = "https://cards.scryfall.io/normal/front/5/2/52e40cb2-d306-4c8d-859b-ac288e9dc78d.jpg?1783930443",
    releaseDate = "2020-07-17",
    rarity = Rarity.UNCOMMON,
)

package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bake into a Pie reprint in Jumpstart. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in ELD's `cards/` package (the card's
 * earliest real printing); this file contributes only the JMP presentation row.
 */
val BakeIntoAPieReprint = Printing(
    oracleId = "1b9ec782-0ba1-41f1-bc39-d3302494ecb3",
    name = "Bake into a Pie",
    setCode = "JMP",
    collectorNumber = "201",
    scryfallId = "2c6e5b25-b721-45ee-894a-697de1310b8c",
    artist = "Zoltan Boros",
    imageUri = "https://cards.scryfall.io/normal/front/2/c/2c6e5b25-b721-45ee-894a-697de1310b8c.jpg?1782706655",
    releaseDate = "2020-07-17",
    rarity = Rarity.COMMON,
)

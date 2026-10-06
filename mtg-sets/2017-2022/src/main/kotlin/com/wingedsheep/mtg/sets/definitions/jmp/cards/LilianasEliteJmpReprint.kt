package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Liliana's Elite reprint in Jumpstart. Canonical [com.wingedsheep.sdk.model.CardDefinition] lives in
 * Eldritch Moon's `cards/` package; this file contributes only presentation data.
 */
val LilianasEliteJmpReprint = Printing(
    oracleId = "b6492082-d0ef-4f7c-a4b1-f8ecf6eb0890",
    name = "Liliana's Elite",
    setCode = "JMP",
    collectorNumber = "250",
    scryfallId = "abd4dbd9-982c-43cf-b14c-c3179427d5a1",
    artist = "Deruchenko Alexander",
    imageUri = "https://cards.scryfall.io/normal/front/a/b/abd4dbd9-982c-43cf-b14c-c3179427d5a1.jpg?1783930418",
    releaseDate = "2020-07-17",
    rarity = Rarity.UNCOMMON,
)

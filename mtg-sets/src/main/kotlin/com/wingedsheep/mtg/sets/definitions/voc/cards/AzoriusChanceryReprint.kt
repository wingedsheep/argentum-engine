package com.wingedsheep.mtg.sets.definitions.voc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Azorius Chancery reprint in Innistrad: Crimson Vow Commander. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Dissension (`dis`) `cards/` package
 * (the card's earliest real printing); this file contributes only per-printing presentation data.
 */
val AzoriusChanceryReprint = Printing(
    oracleId = "189fc8f4-17ac-4f1d-82c8-8401445bdaf4",
    name = "Azorius Chancery",
    setCode = "VOC",
    collectorNumber = "171",
    scryfallId = "26700da8-2b89-4a15-b167-38bfaf7b4348",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/2/6/26700da8-2b89-4a15-b167-38bfaf7b4348.jpg?1783924936",
    releaseDate = "2021-11-19",
    rarity = Rarity.UNCOMMON,
)

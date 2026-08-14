package com.wingedsheep.mtg.sets.definitions.khc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Azorius Chancery reprint in Kaldheim Commander. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Dissension (`dis`) `cards/` package
 * (the card's earliest real printing); this file contributes only per-printing presentation data.
 */
val AzoriusChanceryReprint = Printing(
    oracleId = "189fc8f4-17ac-4f1d-82c8-8401445bdaf4",
    name = "Azorius Chancery",
    setCode = "KHC",
    collectorNumber = "106",
    scryfallId = "caccc497-5312-4a28-8582-aa95bcbfb436",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/c/a/caccc497-5312-4a28-8582-aa95bcbfb436.jpg?1783928297",
    releaseDate = "2021-02-05",
    rarity = Rarity.UNCOMMON,
)

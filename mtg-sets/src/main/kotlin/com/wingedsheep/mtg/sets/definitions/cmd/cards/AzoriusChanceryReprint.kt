package com.wingedsheep.mtg.sets.definitions.cmd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Azorius Chancery reprint in Commander 2011. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Dissension (`dis`) `cards/` package
 * (the card's earliest real printing); this file contributes only per-printing presentation data.
 */
val AzoriusChanceryReprint = Printing(
    oracleId = "189fc8f4-17ac-4f1d-82c8-8401445bdaf4",
    name = "Azorius Chancery",
    setCode = "CMD",
    collectorNumber = "265",
    scryfallId = "9b30408c-4dcf-4959-8bd6-d45397def2cd",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/9/b/9b30408c-4dcf-4959-8bd6-d45397def2cd.jpg?1783941153",
    releaseDate = "2011-06-17",
    rarity = Rarity.COMMON,
)

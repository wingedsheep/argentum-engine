package com.wingedsheep.mtg.sets.definitions.ddl.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Volt Charge reprint in Duel Decks: Heroes vs. Monsters. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the New Phyrexia (`nph`) `cards/`
 * package; this file contributes only per-printing presentation data.
 */
val VoltChargeReprint = Printing(
    oracleId = "c57907d1-c5a1-45c7-80a8-38fedf86dd19",
    name = "Volt Charge",
    setCode = "DDL",
    collectorNumber = "68",
    scryfallId = "8fde462c-8146-4688-a78d-113fc4d42851",
    artist = "Jana Schirmer & Johannes Voss",
    imageUri = "https://cards.scryfall.io/normal/front/8/f/8fde462c-8146-4688-a78d-113fc4d42851.jpg?1783939834",
    releaseDate = "2013-09-06",
    rarity = Rarity.COMMON,
)

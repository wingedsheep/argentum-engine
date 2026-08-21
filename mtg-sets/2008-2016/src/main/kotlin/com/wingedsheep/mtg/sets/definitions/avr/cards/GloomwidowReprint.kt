package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Gloomwidow reprint in Avacyn Restored. The canonical CardDefinition lives in Shadowmoor
 * (`definitions/shm/cards/Gloomwidow.kt`); this file contributes only per-printing presentation data.
 */
val GloomwidowReprint = Printing(
    oracleId = "e5139376-cdb1-4f1e-b417-b956d6b713b9",
    name = "Gloomwidow",
    setCode = "AVR",
    collectorNumber = "180",
    scryfallId = "a016c872-09bd-42e1-94da-f587e8252492",
    artist = "Svetlin Velinov",
    imageUri = "https://cards.scryfall.io/normal/front/a/0/a016c872-09bd-42e1-94da-f587e8252492.jpg?1783940667",
    releaseDate = "2012-05-04",
    rarity = Rarity.UNCOMMON,
)

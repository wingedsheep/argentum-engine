package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lure reprint in MMQ.
 * Canonical CardDefinition lives in Limited Edition Alpha (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.lea.cards.Lure`.
 */
val LureMmqReprint = Printing(
    oracleId = "7a7425ba-4478-4bc4-855f-abf947ea4fa2",
    name = "Lure",
    setCode = "MMQ",
    collectorNumber = "258",
    scryfallId = "89e0015e-9b16-4787-8b4f-02d8bddb1b80",
    artist = "DiTerlizzi",
    imageUri = "https://cards.scryfall.io/normal/front/8/9/89e0015e-9b16-4787-8b4f-02d8bddb1b80.jpg?1783945922",
    releaseDate = "1999-10-04",
    rarity = Rarity.UNCOMMON,
)

package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lure reprint in LEB.
 * Canonical CardDefinition lives in Limited Edition Alpha (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.lea.cards.Lure`.
 */
val LureLebReprint = Printing(
    oracleId = "7a7425ba-4478-4bc4-855f-abf947ea4fa2",
    name = "Lure",
    setCode = "LEB",
    collectorNumber = "212",
    scryfallId = "e31495ab-e6ed-40a6-b82d-aa6092b049e2",
    artist = "Anson Maddocks",
    imageUri = "https://cards.scryfall.io/normal/front/e/3/e31495ab-e6ed-40a6-b82d-aa6092b049e2.jpg?1783948612",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)

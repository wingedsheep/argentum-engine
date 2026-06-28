package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lightning Strike reprint in Avatar: The Last Airbender. Canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Theros (THS); this file contributes
 * only presentation data for the TLA printing.
 */
val LightningStrikeReprint = Printing(
    oracleId = "f34b9bc4-7bfe-47fd-ba23-4eeeb46026eb",
    name = "Lightning Strike",
    setCode = "TLA",
    collectorNumber = "146",
    scryfallId = "5787b0e0-9469-4a6d-8b81-c992628e28c0",
    artist = "Jo Cordisco",
    imageUri = "https://cards.scryfall.io/normal/front/5/7/5787b0e0-9469-4a6d-8b81-c992628e28c0.jpg?1764121006",
    releaseDate = "2025-11-21",
    rarity = Rarity.COMMON,
)

package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sacred Foundry reprint in Edge of Eternities. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Ravnica: City of Guilds (`rav`)
 * `cards/` package (the card's earliest real printing); this file contributes only
 * per-printing presentation data.
 */
val SacredFoundryReprint = Printing(
    oracleId = "45181cb8-2090-4471-ba90-e5a8f04d525f",
    name = "Sacred Foundry",
    setCode = "EOE",
    collectorNumber = "256",
    scryfallId = "8b4e2642-3c87-4708-b9b4-2e7f7359ac7d",
    artist = "Titus Lunter",
    imageUri = "https://cards.scryfall.io/normal/front/8/b/8b4e2642-3c87-4708-b9b4-2e7f7359ac7d.jpg?1752947600",
    releaseDate = "2025-08-01",
    rarity = Rarity.RARE,
)

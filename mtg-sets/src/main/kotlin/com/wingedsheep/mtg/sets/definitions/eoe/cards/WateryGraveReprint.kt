package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Watery Grave reprint in Edge of Eternities. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Ravnica: City of Guilds (`rav`)
 * `cards/` package (the card's earliest real printing); this file contributes only
 * per-printing presentation data.
 */
val WateryGraveReprint = Printing(
    oracleId = "fc9ec820-4245-4a96-b009-5308a818ca58",
    name = "Watery Grave",
    setCode = "EOE",
    collectorNumber = "261",
    scryfallId = "5b8170dc-6a90-46fc-9989-7575f3d402b5",
    artist = "Sergey Glushakov",
    imageUri = "https://cards.scryfall.io/normal/front/5/b/5b8170dc-6a90-46fc-9989-7575f3d402b5.jpg?1752947617",
    releaseDate = "2025-08-01",
    rarity = Rarity.RARE,
)

package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Black Lotus reprint in Limited Edition Beta. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Limited Edition Alpha (`lea`) `cards/`
 * package; this file contributes only per-printing presentation data.
 */
val BlackLotusReprint = Printing(
    oracleId = "5089ec1a-f881-4d55-af14-5d996171203b",
    name = "Black Lotus",
    setCode = "LEB",
    collectorNumber = "233",
    scryfallId = "b3a69a1c-c80f-4413-a6fd-ae54cabbce28",
    artist = "Christopher Rush",
    imageUri = "https://cards.scryfall.io/normal/front/b/3/b3a69a1c-c80f-4413-a6fd-ae54cabbce28.jpg?1783948607",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)

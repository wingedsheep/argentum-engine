package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Commune with Nature reprint in Wilds of Eldraine. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the card's earliest printing, Champions of
 * Kamigawa (`definitions/chk/cards/CommuneWithNature.kt`); this file contributes only WOE's
 * presentation data.
 */
val CommuneWithNatureReprint = Printing(
    oracleId = "d4ed4260-5f38-4b55-ba6e-9f54f04d2360",
    name = "Commune with Nature",
    setCode = "WOE",
    collectorNumber = "166",
    scryfallId = "5224eec3-2941-4d16-a713-099e34e93eee",
    artist = "Jodie Muir",
    imageUri = "https://cards.scryfall.io/normal/front/5/2/5224eec3-2941-4d16-a713-099e34e93eee.jpg?1783915084",
    releaseDate = "2023-09-08",
    rarity = Rarity.COMMON,
)

package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bog Raiders reprint in USG.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * an earlier set's `cards/` package. This file contributes only the USG-specific
 * presentation row.
 */
val BogRaidersReprint = Printing(
    oracleId = "60824fae-20ed-4122-82c9-e99a1b679c54",
    name = "Bog Raiders",
    setCode = "USG",
    collectorNumber = "119",
    scryfallId = "3739188b-f2b3-4ab0-8e5c-b3a1d2a1ad09",
    artist = "Carl Critchlow",
    imageUri = "https://cards.scryfall.io/normal/front/3/7/3739188b-f2b3-4ab0-8e5c-b3a1d2a1ad09.jpg?1562906614",
    releaseDate = "1998-10-12",
    rarity = Rarity.COMMON,
)

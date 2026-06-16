package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Llanowar Wastes reprint in BRO.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in APC's `cards/` package
 * (the card's earliest real printing). This file contributes only the BRO-specific
 * presentation row, surfaced via the set's `printings`.
 */
val LlanowarWastesReprint = Printing(
    oracleId = "32116127-cf96-4a1b-8896-a1ebc087b597",
    name = "Llanowar Wastes",
    setCode = "BRO",
    collectorNumber = "264",
    artist = "Lucas Graciano",
    imageUri = "https://cards.scryfall.io/normal/front/1/0/10716909-1254-4b2b-997e-23a18994a98d.jpg",
    releaseDate = "2022-11-18",
    rarity = Rarity.RARE,
)

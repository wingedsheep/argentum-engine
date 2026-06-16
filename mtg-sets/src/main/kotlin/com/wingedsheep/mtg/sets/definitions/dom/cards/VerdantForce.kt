package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Verdant Force reprint in DOM.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in TMP's `cards/` package
 * (the card's earliest real printing). This file contributes only the DOM-specific
 * presentation row, surfaced via the set's `printings`.
 */
val VerdantForceReprint = Printing(
    oracleId = "7a21ea22-3cd7-4c11-8895-5943c0d93a0d",
    name = "Verdant Force",
    setCode = "DOM",
    collectorNumber = "187",
    artist = "Viktor Titov",
    imageUri = "https://cards.scryfall.io/normal/front/1/d/1d972f97-1945-440b-8bd3-63038db22257.jpg",
    releaseDate = "2018-04-27",
    rarity = Rarity.RARE,
)

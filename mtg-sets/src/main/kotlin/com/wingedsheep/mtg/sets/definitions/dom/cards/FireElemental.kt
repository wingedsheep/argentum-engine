package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fire Elemental reprint in DOM.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes
 * only the DOM-specific presentation row.
 */
val FireElementalReprint = Printing(
    oracleId = "3912d21e-1ebc-4a81-9dc9-f404248d564a",
    name = "Fire Elemental",
    setCode = "DOM",
    collectorNumber = "120",
    artist = "Joe Slucher",
    imageUri = "https://cards.scryfall.io/normal/front/6/2/62405700-d42c-4c96-9678-dd72d7b7c807.jpg?1562736683",
    releaseDate = "2018-04-27",
    rarity = Rarity.COMMON,
)

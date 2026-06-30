package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Skirk Prospector reprint in DOM.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ONS's `cards/` package (the card's earliest real printing). This file contributes
 * only the DOM-specific presentation row.
 */
val SkirkProspectorReprint = Printing(
    oracleId = "c18013e4-0b99-44e3-a2b2-027ace68723a",
    name = "Skirk Prospector",
    setCode = "DOM",
    collectorNumber = "144",
    scryfallId = "1636d138-aa63-476f-a930-41b1be988032",
    artist = "Slawomir Maniak",
    imageUri = "https://cards.scryfall.io/normal/front/1/6/1636d138-aa63-476f-a930-41b1be988032.jpg?1562731846",
    releaseDate = "2018-04-27",
    rarity = Rarity.COMMON,
)

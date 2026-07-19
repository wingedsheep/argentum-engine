package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lightning Strike reprint in Marvel Super Heroes. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Theros (`ths`) `cards/` package;
 * this file contributes only per-printing presentation data.
 */
val LightningStrikeReprint = Printing(
    oracleId = "f34b9bc4-7bfe-47fd-ba23-4eeeb46026eb",
    name = "Lightning Strike",
    setCode = "MSH",
    collectorNumber = "142",
    scryfallId = "88b13bc0-da54-4c3b-917c-7c8345a329f5",
    artist = "Toni Infante",
    imageUri = "https://cards.scryfall.io/normal/front/8/8/88b13bc0-da54-4c3b-917c-7c8345a329f5.jpg?1783902927",
    releaseDate = "2026-06-26",
    rarity = Rarity.COMMON,
)

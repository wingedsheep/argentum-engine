package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Crusader of Odric reprint in Innistrad Remastered (INR). The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Magic 2013's `cards/` package;
 * this file contributes only the INR presentation row.
 */
val CrusaderOfOdricReprint = Printing(
    oracleId = "ea384b0d-3091-4d50-b15f-1f6763647b7c",
    name = "Crusader of Odric",
    setCode = "INR",
    collectorNumber = "18",
    scryfallId = "d6ae6566-3df8-4e09-b487-f0362622ca04",
    artist = "Michael Komarck",
    imageUri = "https://cards.scryfall.io/normal/front/d/6/d6ae6566-3df8-4e09-b487-f0362622ca04.jpg?1782726879",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)

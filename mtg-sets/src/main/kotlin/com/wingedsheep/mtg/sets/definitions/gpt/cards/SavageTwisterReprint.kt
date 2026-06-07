package com.wingedsheep.mtg.sets.definitions.gpt.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Savage Twister reprint in Guildpact. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Mirage set's `cards/`
 * package (Mirage is the card's earliest real printing); this file contributes
 * only presentation data for the Guildpact printing.
 */
val SavageTwisterReprint = Printing(
    oracleId = "5268518e-7631-4149-8eea-c2475b9b14df",
    name = "Savage Twister",
    setCode = "GPT",
    collectorNumber = "127",
    scryfallId = "682ee5a9-2995-4868-b7ea-8735b2aee77e",
    artist = "Luca Zontini",
    imageUri = "https://cards.scryfall.io/normal/front/6/8/682ee5a9-2995-4868-b7ea-8735b2aee77e.jpg?1593272763",
    releaseDate = "2006-02-03",
    rarity = Rarity.UNCOMMON,
)

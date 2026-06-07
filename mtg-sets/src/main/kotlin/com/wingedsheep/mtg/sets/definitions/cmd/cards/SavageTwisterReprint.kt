package com.wingedsheep.mtg.sets.definitions.cmd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Savage Twister reprint in Commander 2011. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Mirage set's `cards/`
 * package (Mirage is the card's earliest real printing); this file contributes
 * only presentation data for the Commander 2011 printing.
 */
val SavageTwisterReprint = Printing(
    oracleId = "5268518e-7631-4149-8eea-c2475b9b14df",
    name = "Savage Twister",
    setCode = "CMD",
    collectorNumber = "222",
    scryfallId = "5aa253a6-2513-4dfd-b34a-88c341d0372c",
    artist = "Bob Eggleton",
    imageUri = "https://cards.scryfall.io/normal/front/5/a/5aa253a6-2513-4dfd-b34a-88c341d0372c.jpg?1592714244",
    releaseDate = "2011-06-17",
    rarity = Rarity.UNCOMMON,
)

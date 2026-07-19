package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Gingerbrute reprint in Wilds of Eldraine. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Throne of Eldraine (ELD) package
 * (its earliest real printing); this file contributes only WOE presentation data.
 */
val GingerbruteReprint = Printing(
    oracleId = "10b8d4c7-7553-4d76-b643-d98b80701e13",
    name = "Gingerbrute",
    setCode = "WOE",
    collectorNumber = "246",
    scryfallId = "09a4578a-7dc6-4da3-93ee-913b10be5740",
    artist = "Carlos Palma Cruchaga",
    imageUri = "https://cards.scryfall.io/normal/front/0/9/09a4578a-7dc6-4da3-93ee-913b10be5740.jpg?1783915058",
    releaseDate = "2023-09-08",
    rarity = Rarity.COMMON,
)

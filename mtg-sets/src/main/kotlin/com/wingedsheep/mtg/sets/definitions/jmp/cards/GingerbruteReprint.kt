package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Gingerbrute reprint in Jumpstart (JMP). The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Throne of Eldraine (ELD) package
 * (its earliest real printing); this file contributes only JMP presentation data.
 */
val GingerbruteReprint = Printing(
    oracleId = "10b8d4c7-7553-4d76-b643-d98b80701e13",
    name = "Gingerbrute",
    setCode = "JMP",
    collectorNumber = "466",
    scryfallId = "c1195ec5-979b-4c4a-9c04-62bb53c2b011",
    artist = "Vincent Proce",
    imageUri = "https://cards.scryfall.io/normal/front/c/1/c1195ec5-979b-4c4a-9c04-62bb53c2b011.jpg?1783930339",
    releaseDate = "2020-07-17",
    rarity = Rarity.COMMON,
)

package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wicked Guardian reprint in J22. Canonical CardDefinition lives in Throne of Eldraine (its earliest
 * real printing), `com.wingedsheep.mtg.sets.definitions.eld.cards.WickedGuardian`.
 */
val WickedGuardianReprint = Printing(
    oracleId = "dda70e25-b3c5-444d-9dff-1366771bf881",
    name = "Wicked Guardian",
    setCode = "J22",
    collectorNumber = "489",
    scryfallId = "b84c6b02-64c6-486d-b38b-be0f7327e9c6",
    artist = "Matt Stewart",
    imageUri = "https://cards.scryfall.io/normal/front/b/8/b84c6b02-64c6-486d-b38b-be0f7327e9c6.jpg?1783918969",
    releaseDate = "2022-12-02",
    rarity = Rarity.COMMON,
)

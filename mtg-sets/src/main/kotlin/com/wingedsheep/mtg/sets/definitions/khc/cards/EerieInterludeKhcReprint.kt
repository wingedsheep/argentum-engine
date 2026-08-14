package com.wingedsheep.mtg.sets.definitions.khc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Eerie Interlude reprint in KHC. Canonical CardDefinition lives in Duel Decks: Blessed vs. Cursed (its earliest real
 * printing), `com.wingedsheep.mtg.sets.definitions.ddq.cards.EerieInterlude`.
 */
val EerieInterludeKhcReprint = Printing(
    oracleId = "0634091a-a74c-4cea-b6d1-7324a725554a",
    name = "Eerie Interlude",
    setCode = "KHC",
    collectorNumber = "22",
    scryfallId = "4ba9f15f-00d2-4797-9228-91b320e85705",
    artist = "Svetlin Velinov",
    imageUri = "https://cards.scryfall.io/normal/front/4/b/4ba9f15f-00d2-4797-9228-91b320e85705.jpg?1783928332",
    releaseDate = "2021-02-05",
    rarity = Rarity.RARE,
)
